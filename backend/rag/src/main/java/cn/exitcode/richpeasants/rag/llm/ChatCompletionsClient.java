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
import java.util.TreeMap;
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
        return complete(model, systemPrompt, userPrompt, null);
    }

    public String complete(LlmModel model, String systemPrompt, String userPrompt, Double temperature) {
        StringBuilder full = new StringBuilder();
        stream(model, systemPrompt, userPrompt, full::append, temperature);
        String answer = full.toString().trim();
        if (!StringUtils.hasText(answer)) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "对话模型返回空内容");
        }
        return answer;
    }

    public String complete(LlmModel model, List<Map<String, Object>> messages) {
        return complete(model, messages, null);
    }

    public String complete(LlmModel model, List<Map<String, Object>> messages, Double temperature) {
        StringBuilder full = new StringBuilder();
        stream(model, messages, full::append, temperature);
        String answer = full.toString().trim();
        if (!StringUtils.hasText(answer)) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "对话模型返回空内容");
        }
        return answer;
    }

    public void stream(LlmModel model, String systemPrompt, String userPrompt, Consumer<String> onDelta) {
        stream(model, systemPrompt, userPrompt, onDelta, null);
    }

    public void stream(LlmModel model,
                       String systemPrompt,
                       String userPrompt,
                       Consumer<String> onDelta,
                       Double temperature) {
        List<Map<String, Object>> messages = new ArrayList<>();
        if (StringUtils.hasText(systemPrompt)) {
            messages.add(Map.of("role", "system", "content", systemPrompt));
        }
        messages.add(Map.of("role", "user", "content", userPrompt));
        stream(model, messages, onDelta, temperature);
    }

    /**
     * 流式调用（完整 messages，无 tools）。
     */
    public void stream(LlmModel model, List<Map<String, Object>> messages, Consumer<String> onDelta) {
        stream(model, messages, onDelta, null);
    }

    public void stream(LlmModel model,
                       List<Map<String, Object>> messages,
                       Consumer<String> onDelta,
                       Double temperature) {
        String url = completionsUrl(model);
        Map<String, Object> body = baseBody(model, messages, temperature);
        body.put("stream", true);

        try {
            log.debug("Calling chat completions(stream): url={}, model={}", url, body.get("model"));
            chatRestClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.TEXT_EVENT_STREAM)
                    .headers(headers -> applyAuth(headers, model))
                    .body(body)
                    .exchange((request, response) -> {
                        if (response.getStatusCode().isError()) {
                            throw httpError("对话模型流式调用失败", response);
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
                                if (payload.isEmpty()) {
                                    continue;
                                }
                                if ("[DONE]".equals(payload)) {
                                    break;
                                }
                                String delta;
                                try {
                                    delta = extractDelta(payload);
                                } catch (Exception parseEx) {
                                    log.debug("Skip bad SSE chunk: {}", parseEx.getMessage());
                                    continue;
                                }
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

    /**
     * 流式一轮：可带 tools。正文 delta 实时回调；同时累积 tool_calls 供 Agent 判断。
     * 一旦出现 tool_calls 分片，停止向 onDelta 推送正文（避免工具轮次误输出）。
     */
    public AssistantMessage streamTurn(LlmModel model,
                                       List<Map<String, Object>> messages,
                                       List<Map<String, Object>> tools,
                                       Consumer<String> onDelta) {
        String url = completionsUrl(model);
        Map<String, Object> body = baseBody(model, messages);
        body.put("stream", true);
        if (tools != null && !tools.isEmpty()) {
            body.put("tools", tools);
            body.put("tool_choice", "auto");
        }

        StringBuilder content = new StringBuilder();
        Map<Integer, MutableToolCall> toolBuilders = new TreeMap<>();
        boolean[] forwardContent = {true};

        try {
            log.debug("Calling chat completions(streamTurn): url={}, model={}, tools={}",
                    url, body.get("model"), tools == null ? 0 : tools.size());
            chatRestClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.TEXT_EVENT_STREAM)
                    .headers(headers -> applyAuth(headers, model))
                    .body(body)
                    .exchange((request, response) -> {
                        if (response.getStatusCode().isError()) {
                            throw httpError("对话模型流式调用失败", response);
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
                                try {
                                    mergeStreamChunk(payload, content, toolBuilders, forwardContent, onDelta);
                                } catch (Exception parseEx) {
                                    log.debug("Skip bad SSE chunk: {}", parseEx.getMessage());
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

        List<ToolCall> toolCalls = toToolCalls(toolBuilders);
        String text = content.length() == 0 ? null : content.toString();
        return new AssistantMessage(text, toolCalls);
    }

    /**
     * 非流式一轮：可带 tools（保留作兜底；Agent 主路径请用 streamTurn）。
     */
    public AssistantMessage completeTurn(LlmModel model,
                                         List<Map<String, Object>> messages,
                                         List<Map<String, Object>> tools) {
        String url = completionsUrl(model);
        Map<String, Object> body = baseBody(model, messages);
        body.put("stream", false);
        if (tools != null && !tools.isEmpty()) {
            body.put("tools", tools);
            body.put("tool_choice", "auto");
        }

        try {
            log.debug("Calling chat completions(turn): url={}, model={}, tools={}",
                    url, body.get("model"), tools == null ? 0 : tools.size());
            String response = chatRestClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(headers -> applyAuth(headers, model))
                    .body(body)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(response == null ? "{}" : response);
            JsonNode message = root.path("choices").path(0).path("message");
            if (message.isMissingNode() || message.isNull()) {
                throw new BusinessException(ResultCode.INTERNAL_ERROR, "对话模型未返回 message");
            }

            String content = null;
            JsonNode contentNode = message.get("content");
            if (contentNode != null && !contentNode.isNull()) {
                content = contentNode.isTextual() ? contentNode.textValue() : contentNode.asText();
            }

            List<ToolCall> toolCalls = new ArrayList<>();
            JsonNode toolCallsNode = message.get("tool_calls");
            if (toolCallsNode != null && toolCallsNode.isArray()) {
                int fallback = 0;
                for (JsonNode item : toolCallsNode) {
                    String id = item.path("id").asText("");
                    String name = item.path("function").path("name").asText("");
                    String args = item.path("function").path("arguments").asText("{}");
                    if (!StringUtils.hasText(id)) {
                        id = "call_" + (++fallback);
                    }
                    if (StringUtils.hasText(name)) {
                        toolCalls.add(new ToolCall(id, name, args));
                    }
                }
            }
            return new AssistantMessage(content, toolCalls);
        } catch (BusinessException ex) {
            throw ex;
        } catch (ResourceAccessException ex) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR,
                    "调用对话模型超时或网络不可达（可调大 app.rag.read-timeout-ms）: " + ex.getMessage());
        } catch (Exception ex) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "调用对话模型失败: " + ex.getMessage());
        }
    }

    private Map<String, Object> baseBody(LlmModel model, List<Map<String, Object>> messages) {
        return baseBody(model, messages, null);
    }

    private Map<String, Object> baseBody(LlmModel model, List<Map<String, Object>> messages, Double temperature) {
        String modelName = StringUtils.hasText(model.getModelName()) ? model.getModelName() : model.getName();
        Map<String, Object> body = new HashMap<>();
        body.put("model", modelName);
        body.put("messages", messages);
        body.put("temperature", temperature != null ? temperature : ragAppProperties.getTemperature());
        return body;
    }

    private String completionsUrl(LlmModel model) {
        return trimSlash(model.getBaseUrl()) + "/chat/completions";
    }

    private void applyAuth(org.springframework.http.HttpHeaders headers, LlmModel model) {
        if (StringUtils.hasText(model.getApiKey())) {
            headers.setBearerAuth(model.getApiKey());
        }
    }

    private BusinessException httpError(String prefix, RestClient.RequestHeadersSpec.ConvertibleClientHttpResponse response) {
        String errBody = "";
        int status = -1;
        try {
            status = response.getStatusCode().value();
        } catch (Exception ignored) {
            // ignore
        }
        try (InputStream err = response.getBody()) {
            if (err != null) {
                errBody = new String(err.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (Exception ignored) {
            // ignore
        }
        return new BusinessException(ResultCode.INTERNAL_ERROR,
                prefix + " HTTP " + status
                        + (StringUtils.hasText(errBody) ? (": " + truncate(errBody, 300)) : ""));
    }

    private void mergeStreamChunk(String payload,
                                  StringBuilder content,
                                  Map<Integer, MutableToolCall> toolBuilders,
                                  boolean[] forwardContent,
                                  Consumer<String> onDelta) throws Exception {
        JsonNode root = objectMapper.readTree(payload);
        JsonNode delta = root.path("choices").path(0).path("delta");

        JsonNode toolCallsNode = delta.get("tool_calls");
        if (toolCallsNode != null && toolCallsNode.isArray() && !toolCallsNode.isEmpty()) {
            forwardContent[0] = false;
            for (JsonNode item : toolCallsNode) {
                int index = item.path("index").asInt(toolBuilders.size());
                MutableToolCall builder = toolBuilders.computeIfAbsent(index, ignored -> new MutableToolCall());
                if (item.hasNonNull("id")) {
                    String id = item.get("id").asText("");
                    if (StringUtils.hasText(id)) {
                        builder.id = id;
                    }
                }
                JsonNode fn = item.get("function");
                if (fn != null && !fn.isNull()) {
                    if (fn.hasNonNull("name")) {
                        String name = fn.get("name").asText("");
                        if (StringUtils.hasText(name)) {
                            builder.name = name;
                        }
                    }
                    if (fn.hasNonNull("arguments")) {
                        builder.arguments.append(fn.get("arguments").asText(""));
                    }
                }
            }
        }

        JsonNode contentNode = delta.get("content");
        if (contentNode == null || contentNode.isNull()) {
            return;
        }
        String piece = contentNode.isTextual() ? contentNode.textValue() : contentNode.asText();
        if (piece == null || piece.isEmpty()) {
            return;
        }
        content.append(piece);
        if (forwardContent[0] && onDelta != null) {
            onDelta.accept(piece);
        }
    }

    private List<ToolCall> toToolCalls(Map<Integer, MutableToolCall> toolBuilders) {
        List<ToolCall> toolCalls = new ArrayList<>();
        int fallback = 0;
        for (MutableToolCall builder : toolBuilders.values()) {
            if (!StringUtils.hasText(builder.name)) {
                continue;
            }
            String id = StringUtils.hasText(builder.id) ? builder.id : "call_" + (++fallback);
            String args = builder.arguments.length() == 0 ? "{}" : builder.arguments.toString();
            toolCalls.add(new ToolCall(id, builder.name, args));
        }
        return toolCalls;
    }

    private static final class MutableToolCall {
        private String id = "";
        private String name = "";
        private final StringBuilder arguments = new StringBuilder();
    }

    private String extractDelta(String payload) throws Exception {
        JsonNode root = objectMapper.readTree(payload);
        JsonNode content = root.path("choices").path(0).path("delta").path("content");
        if (content.isMissingNode() || content.isNull()) {
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

    public record ToolCall(String id, String name, String argumentsJson) {
    }

    public record AssistantMessage(String content, List<ToolCall> toolCalls) {
        public boolean hasToolCalls() {
            return toolCalls != null && !toolCalls.isEmpty();
        }
    }
}
