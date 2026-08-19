package cn.exitcode.richpeasants.rag.llm;

import cn.exitcode.richpeasants.common.entity.LlmModel;
import cn.exitcode.richpeasants.common.exception.BusinessException;
import cn.exitcode.richpeasants.common.result.ResultCode;
import cn.exitcode.richpeasants.rag.config.RagAppProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * OpenAI 兼容 /chat/completions 客户端（自研，无 LangChain4j）。
 */
@Service
public class ChatCompletionsClient {

    private static final Logger log = LoggerFactory.getLogger(ChatCompletionsClient.class);

    private final RestClient chatRestClient;
    private final ObjectMapper objectMapper;
    private final RagAppProperties ragAppProperties;

    public ChatCompletionsClient(@Qualifier("chatRestClient") RestClient chatRestClient,
                                 ObjectMapper objectMapper,
                                 RagAppProperties ragAppProperties) {
        this.chatRestClient = chatRestClient;
        this.objectMapper = objectMapper;
        this.ragAppProperties = ragAppProperties;
    }

    public String complete(LlmModel model, String systemPrompt, String userPrompt) {
        StringBuilder full = new StringBuilder();
        stream(model, systemPrompt, userPrompt, full::append);
        String answer = full.toString().trim();
        if (!StringUtils.hasText(answer)) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "对话模型返回空内容");
        }
        return answer;
    }

    /**
     * 流式调用：上游 SSE 按 delta 回调；阻塞直到结束。
     */
    public void stream(LlmModel model, String systemPrompt, String userPrompt, Consumer<String> onDelta) {
        String baseUrl = trimSlash(model.getBaseUrl());
        String modelName = StringUtils.hasText(model.getModelName()) ? model.getModelName() : model.getName();
        String url = baseUrl + "/chat/completions";

        List<Map<String, String>> messages = new ArrayList<>();
        if (StringUtils.hasText(systemPrompt)) {
            messages.add(Map.of("role", "system", "content", systemPrompt));
        }
        messages.add(Map.of("role", "user", "content", userPrompt));

        Map<String, Object> body = new HashMap<>();
        body.put("model", modelName);
        body.put("messages", messages);
        body.put("temperature", ragAppProperties.getTemperature());
        body.put("stream", true);

        try {
            log.debug("Calling chat completions(stream): url={}, model={}", url, modelName);
            chatRestClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.TEXT_EVENT_STREAM)
                    .headers(headers -> {
                        if (StringUtils.hasText(model.getApiKey())) {
                            headers.setBearerAuth(model.getApiKey());
                        }
                    })
                    .body(body)
                    .exchange((request, response) -> {
                        if (response.getStatusCode().isError()) {
                            String errBody = "";
                            try (InputStream err = response.getBody()) {
                                if (err != null) {
                                    errBody = new String(err.readAllBytes(), StandardCharsets.UTF_8);
                                }
                            }
                            throw new BusinessException(ResultCode.INTERNAL_ERROR,
                                    "对话模型流式调用失败 HTTP " + response.getStatusCode().value()
                                            + (StringUtils.hasText(errBody) ? (": " + truncate(errBody, 300)) : ""));
                        }
                        try (InputStream in = response.getBody();
                             BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                if (line.startsWith(":") || line.isEmpty()) {
                                    continue;
                                }
                                if (!line.startsWith("data:")) {
                                    continue;
                                }
                                String payload = stripSseDataPrefix(line);
                                if (payload.isEmpty() || "[DONE]".equals(payload)) {
                                    if ("[DONE]".equals(payload)) {
                                        break;
                                    }
                                    continue;
                                }
                                String delta;
                                try {
                                    delta = extractDelta(payload);
                                } catch (Exception parseEx) {
                                    log.debug("Skip bad SSE chunk: {}", parseEx.getMessage());
                                    continue;
                                }
                                // 不能用 hasText：单独的 "\n" / "\n\n" 必须原样转发给前端，否则 ```java 会粘成 javapublic
                                if (delta != null && !delta.isEmpty()) {
                                    onDelta.accept(delta);
                                }
                            }
                        }
                        return null;
                    });
        } catch (BusinessException ex) {
            throw ex;
        } catch (ResourceAccessException ex) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR,
                    "调用对话模型超时或网络不可达（可调大 app.rag.read-timeout-ms）: " + ex.getMessage());
        } catch (Exception ex) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "调用对话模型失败: " + ex.getMessage());
        }
    }

    private String extractDelta(String payload) throws Exception {
        JsonNode root = objectMapper.readTree(payload);
        JsonNode content = root.path("choices").path(0).path("delta").path("content");
        if (content.isMissingNode() || content.isNull()) {
            // 部分兼容接口非流式字段
            content = root.path("choices").path(0).path("message").path("content");
        }
        if (content.isMissingNode() || content.isNull()) {
            return null;
        }
        if (content.isTextual()) {
            return content.textValue();
        }
        return content.asText();
    }

    /** SSE spec：`data:` 后最多一个可选空格，后面原样保留（含 JSON 里的 \\n）。 */
    private static String stripSseDataPrefix(String line) {
        String value = line.substring(5);
        if (!value.isEmpty() && value.charAt(0) == ' ') {
            return value.substring(1);
        }
        return value;
    }

    private static String truncate(String text, int max) {
        if (text == null || text.length() <= max) {
            return text == null ? "" : text;
        }
        return text.substring(0, max) + "…";
    }

    private String trimSlash(String url) {
        String value = url == null ? "" : url.trim();
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }
}
