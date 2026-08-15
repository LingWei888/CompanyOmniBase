package cn.exitcode.richpeasants.ingest.embedding;

import cn.exitcode.richpeasants.common.entity.LlmModel;
import cn.exitcode.richpeasants.common.enums.LlmModelPurpose;
import cn.exitcode.richpeasants.common.exception.BusinessException;
import cn.exitcode.richpeasants.common.repository.LlmModelRepository;
import cn.exitcode.richpeasants.common.result.ResultCode;
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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenAI 兼容 /v1/embeddings 客户端。
 */
@Service
public class EmbeddingClient {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingClient.class);

    private final LlmModelRepository llmModelRepository;
    private final RestClient embeddingRestClient;
    private final ObjectMapper objectMapper;

    public EmbeddingClient(LlmModelRepository llmModelRepository,
                           @Qualifier("embeddingRestClient") RestClient embeddingRestClient,
                           ObjectMapper objectMapper) {
        this.llmModelRepository = llmModelRepository;
        this.embeddingRestClient = embeddingRestClient;
        this.objectMapper = objectMapper;
    }

    public LlmModel requireEmbeddingModel() {
        return llmModelRepository.findFirstByPurposeAndEnabledTrueOrderByIdAsc(LlmModelPurpose.EMBEDDING)
                .orElseThrow(() -> new BusinessException(ResultCode.BAD_REQUEST,
                        "未配置启用的 Embedding 模型，请先在「模型管理」中新增用途为向量化的模型"));
    }

    public List<float[]> embed(LlmModel model, List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }
        if (model.getEmbeddingDimension() == null || model.getEmbeddingDimension() <= 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Embedding 模型未配置向量维度");
        }
        String baseUrl = trimSlash(model.getBaseUrl());
        String modelName = StringUtils.hasText(model.getModelName()) ? model.getModelName() : "text-embedding-3-small";
        String url = baseUrl + "/embeddings";

        Map<String, Object> body = new HashMap<>();
        body.put("model", modelName);
        body.put("input", texts);

        try {
            log.debug("Calling embedding API: url={}, model={}, batch={}", url, modelName, texts.size());
            String response = embeddingRestClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(headers -> {
                        if (StringUtils.hasText(model.getApiKey())) {
                            headers.setBearerAuth(model.getApiKey());
                        }
                    })
                    .body(body)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(response);
            JsonNode data = root.get("data");
            if (data == null || !data.isArray() || data.isEmpty()) {
                throw new BusinessException(ResultCode.INTERNAL_ERROR, "Embedding 接口未返回向量数据");
            }

            List<JsonNode> items = new ArrayList<>();
            data.forEach(items::add);
            // OpenAI 兼容接口可能乱序返回，按 index 还原
            items.sort(Comparator.comparingInt(item -> item.path("index").asInt(0)));

            List<float[]> vectors = new ArrayList<>(items.size());
            for (JsonNode item : items) {
                JsonNode embedding = item.get("embedding");
                if (embedding == null || !embedding.isArray()) {
                    throw new BusinessException(ResultCode.INTERNAL_ERROR, "Embedding 返回格式无效");
                }
                float[] vector = new float[embedding.size()];
                for (int i = 0; i < embedding.size(); i++) {
                    vector[i] = (float) embedding.get(i).asDouble();
                }
                if (vector.length != model.getEmbeddingDimension()) {
                    throw new BusinessException(ResultCode.BAD_REQUEST,
                            "向量维度不匹配：模型配置 embedding_dimension=" + model.getEmbeddingDimension()
                                    + "，接口实际返回 " + vector.length
                                    + "。请在「模型管理」改成与硅基流动该模型一致的维度，并重新入库");
                }
                vectors.add(vector);
            }
            if (vectors.size() != texts.size()) {
                throw new BusinessException(ResultCode.INTERNAL_ERROR,
                        "Embedding 返回条数与输入不一致");
            }
            return vectors;
        } catch (BusinessException ex) {
            throw ex;
        } catch (ResourceAccessException ex) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR,
                    "调用 Embedding 接口超时或网络不可达（可调大 app.embedding.read-timeout-ms / 减小 batch-size）: "
                            + ex.getMessage());
        } catch (Exception ex) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "调用 Embedding 接口失败: " + ex.getMessage());
        }
    }

    private String trimSlash(String url) {
        String value = url == null ? "" : url.trim();
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }
}
