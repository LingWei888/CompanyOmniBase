package cn.exitcode.richpeasants.rag.agent;

import cn.exitcode.richpeasants.rag.config.RagAppProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class WebSearchTool implements AgentTool {

    private final RestClient agentRestClient;
    private final RagAppProperties ragAppProperties;
    private final ObjectMapper objectMapper;

    public WebSearchTool(@Qualifier("agentRestClient") RestClient agentRestClient,
                         RagAppProperties ragAppProperties,
                         ObjectMapper objectMapper) {
        this.agentRestClient = agentRestClient;
        this.ragAppProperties = ragAppProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    public String name() {
        return "web_search";
    }

    @Override
    public String description() {
        return "使用 Tavily 搜索互联网，获取最新公开信息。需要实时新闻、资料或网页事实时调用。";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        Map<String, Object> query = new LinkedHashMap<>();
        query.put("type", "string");
        query.put("description", "搜索关键词或问题");

        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("query", query);

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", java.util.List.of("query"));
        schema.put("additionalProperties", false);
        return schema;
    }

    @Override
    public String execute(JsonNode arguments) throws Exception {
        String apiKey = ragAppProperties.getAgent().getTavily().getApiKey();
        if (!StringUtils.hasText(apiKey)) {
            return "网页搜索未配置：请在 application.yml 设置 app.rag.agent.tavily.api-key（或环境变量 TAVILY_API_KEY）";
        }
        String query = arguments == null ? "" : arguments.path("query").asText("").trim();
        if (!StringUtils.hasText(query)) {
            return "缺少搜索参数 query";
        }

        int maxResults = Math.max(1, Math.min(10, ragAppProperties.getAgent().getTavily().getMaxResults()));
        String baseUrl = trimSlash(ragAppProperties.getAgent().getTavily().getBaseUrl());

        Map<String, Object> body = new HashMap<>();
        body.put("api_key", apiKey);
        body.put("query", query);
        body.put("max_results", maxResults);
        body.put("search_depth", "basic");
        body.put("include_answer", false);

        String response = agentRestClient.post()
                .uri(baseUrl + "/search")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class);

        JsonNode root = objectMapper.readTree(response == null ? "{}" : response);
        JsonNode results = root.path("results");
        if (!results.isArray() || results.isEmpty()) {
            return "未搜索到与「" + query + "」相关的结果";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("搜索词：").append(query).append('\n');
        int index = 1;
        for (JsonNode item : results) {
            if (index > maxResults) {
                break;
            }
            String title = item.path("title").asText("");
            String url = item.path("url").asText("");
            String content = item.path("content").asText("");
            sb.append(index).append(". ").append(title).append('\n');
            if (StringUtils.hasText(url)) {
                sb.append("   链接：").append(url).append('\n');
            }
            if (StringUtils.hasText(content)) {
                String snippet = content.length() > 280 ? content.substring(0, 280) + "…" : content;
                sb.append("   摘要：").append(snippet).append('\n');
            }
            index++;
        }
        return sb.toString().trim();
    }

    private static String trimSlash(String url) {
        String value = url == null ? "" : url.trim();
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }
}
