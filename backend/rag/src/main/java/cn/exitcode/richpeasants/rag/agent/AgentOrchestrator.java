package cn.exitcode.richpeasants.rag.agent;

import cn.exitcode.richpeasants.common.entity.LlmModel;
import cn.exitcode.richpeasants.common.exception.BusinessException;
import cn.exitcode.richpeasants.common.result.ResultCode;
import cn.exitcode.richpeasants.rag.config.RagAppProperties;
import cn.exitcode.richpeasants.rag.llm.ChatCompletionsClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * 简易 Agent：OpenAI tool_calls 循环（时间 / 天气 / Tavily 搜索）。
 */
@Service
public class AgentOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(AgentOrchestrator.class);

    public static final String AGENT_SYSTEM_PROMPT = """
            你是智能助手，可以使用工具获取实时信息后再回答。
            规则：
            1. 若有多轮对话历史，可结合上文理解指代与追问。
            2. 询问当前时间/日期/星期时，调用一次 get_current_time。
            3. 询问天气时，只调用一次 get_weather；禁止再调用 web_search。
            4. 只有明确需要联网查新闻/网页资料时，才调用一次 web_search。
            5. 每一轮每种工具最多调用一次，禁止并行重复同一工具。
            6. 拿到工具结果后立刻用简洁中文回答，不要再调工具。
            7. 禁止输出 DSML / XML / tool_calls 标记；代码用 Markdown 围栏。
            """;

    private static final String FINALIZE_USER_PROMPT = """
            请仅根据上面的工具返回结果，用简洁中文直接回答用户最初的问题。
            禁止再次调用任何工具；禁止输出 tool_calls、DSML、XML、函数调用或类似标记。
            只输出最终回答正文。
            """;

    private final ChatCompletionsClient chatCompletionsClient;
    private final AgentToolRegistry agentToolRegistry;
    private final RagAppProperties ragAppProperties;
    private final ObjectMapper objectMapper;

    public AgentOrchestrator(ChatCompletionsClient chatCompletionsClient,
                             AgentToolRegistry agentToolRegistry,
                             RagAppProperties ragAppProperties,
                             ObjectMapper objectMapper) {
        this.chatCompletionsClient = chatCompletionsClient;
        this.agentToolRegistry = agentToolRegistry;
        this.ragAppProperties = ragAppProperties;
        this.objectMapper = objectMapper;
    }

    /**
     * @param systemPrompt 可含长期记忆的系统提示；为空则用默认 Agent 提示
     * @param history 同会话短期记忆（user/assistant），不含本轮问题
     * @param onTool  工具真正开始执行时回调 (toolName, displayText)；重复/跳过不会触发
     * @param onDelta 最终答案分片
     */
    public String run(LlmModel model,
                      String userQuestion,
                      BiConsumer<String, String> onTool,
                      Consumer<String> onDelta) {
        return run(model, AGENT_SYSTEM_PROMPT, List.of(), userQuestion, onTool, onDelta);
    }

    public String run(LlmModel model,
                      List<Map<String, Object>> history,
                      String userQuestion,
                      BiConsumer<String, String> onTool,
                      Consumer<String> onDelta) {
        return run(model, AGENT_SYSTEM_PROMPT, history, userQuestion, onTool, onDelta);
    }

    public String run(LlmModel model,
                      String systemPrompt,
                      List<Map<String, Object>> history,
                      String userQuestion,
                      BiConsumer<String, String> onTool,
                      Consumer<String> onDelta) {
        List<Map<String, Object>> messages = new ArrayList<>();
        String system = StringUtils.hasText(systemPrompt) ? systemPrompt : AGENT_SYSTEM_PROMPT;
        messages.add(Map.of("role", "system", "content", system));
        if (history != null && !history.isEmpty()) {
            messages.addAll(history);
        }
        messages.add(Map.of("role", "user", "content", userQuestion));

        List<Map<String, Object>> tools = toolsForQuestion(userQuestion);
        int maxRounds = Math.max(1, ragAppProperties.getAgent().getMaxToolRounds());

        // 本轮对话内的工具结果缓存，避免模型并行/多轮重复调用
        Map<String, String> resultByFingerprint = new LinkedHashMap<>();
        Map<String, String> resultByToolName = new LinkedHashMap<>();
        boolean weatherSucceeded = false;

        for (int round = 0; round < maxRounds; round++) {
            ChatCompletionsClient.AssistantMessage turn =
                    chatCompletionsClient.streamTurn(model, messages, tools, onDelta);

            if (!turn.hasToolCalls()) {
                String content = safeText(turn.content());
                if (StringUtils.hasText(content) && !looksLikeToolMarkup(content)) {
                    return content;
                }
                log.info("Agent turn without structured tool_calls but markup/empty; finalize natural answer");
                return finalizeNaturalAnswer(model, messages, onDelta);
            }

            // 若本轮全部是重复/应跳过的调用，直接收尾，别再空转
            if (!hasNewExecutableCall(turn.toolCalls(), resultByFingerprint, resultByToolName, weatherSucceeded)) {
                log.info("Agent tool calls are all duplicates/skips; finalize");
                return finalizeNaturalAnswer(model, messages, onDelta);
            }

            messages.add(toAssistantToolCallMessage(turn));

            // 同一轮内按工具名去重；先跑天气/时间，再跑搜索，避免并行滥调 web_search
            Set<String> namesInThisRound = new LinkedHashSet<>();
            int executedThisRound = 0;
            List<ChatCompletionsClient.ToolCall> orderedCalls = orderToolCalls(turn.toolCalls());

            for (ChatCompletionsClient.ToolCall call : orderedCalls) {
                String name = call.name() == null ? "" : call.name();
                String fingerprint = fingerprint(call);

                String result;
                boolean realExecute = false;

                if (resultByFingerprint.containsKey(fingerprint)
                        || resultByToolName.containsKey(name)
                        || namesInThisRound.contains(name)) {
                    result = resultByFingerprint.getOrDefault(fingerprint,
                            resultByToolName.getOrDefault(name,
                                    "（重复调用已忽略）请直接根据已有工具结果回答。"));
                    if (!result.startsWith("（重复")) {
                        result = "（重复调用已忽略，复用上次结果）\n" + result;
                    }
                    log.debug("Skip duplicate tool call: {}", fingerprint);
                } else if ("web_search".equals(name) && weatherSucceeded) {
                    // 天气已查到时，禁止再为同一问题去搜网页
                    result = "已有 get_weather 结果，请直接据此回答天气问题，不要再 web_search。";
                    log.info("Skip web_search because weather already succeeded");
                } else {
                    namesInThisRound.add(name);
                    if (onTool != null) {
                        onTool.accept(name, toolDisplayName(name));
                    }
                    result = executeTool(call);
                    realExecute = true;
                    executedThisRound++;
                    resultByFingerprint.put(fingerprint, result);
                    // 时间/天气整次对话只保留一次即可
                    if ("get_weather".equals(name) || "get_current_time".equals(name)) {
                        resultByToolName.put(name, result);
                    }
                    if ("get_weather".equals(name) && isToolSuccess(result)) {
                        weatherSucceeded = true;
                    }
                }

                Map<String, Object> toolMsg = new LinkedHashMap<>();
                toolMsg.put("role", "tool");
                toolMsg.put("tool_call_id", call.id());
                toolMsg.put("content", result);
                messages.add(toolMsg);

                // 标记，避免同轮后续同名再真实执行
                if (realExecute) {
                    namesInThisRound.add(name);
                } else {
                    namesInThisRound.add(name);
                }
            }

            // 天气已成功拿到：不再给模型下一轮调工具的机会（常见滥调 web_search）
            if (weatherSucceeded && executedThisRound > 0) {
                log.info("Weather tool succeeded; finalize natural answer without more tool rounds");
                return finalizeNaturalAnswer(model, messages, onDelta);
            }
        }

        log.info("Agent reached max tool rounds={}, forcing natural answer", maxRounds);
        return finalizeNaturalAnswer(model, messages, onDelta);
    }

    /** 天气类问题只暴露 get_weather，避免模型顺手再 web_search */
    private List<Map<String, Object>> toolsForQuestion(String userQuestion) {
        if (looksLikeWeatherQuestion(userQuestion)) {
            return agentToolRegistry.openAiToolsPayload(List.of("get_weather", "get_current_time"));
        }
        if (looksLikeTimeQuestion(userQuestion)) {
            return agentToolRegistry.openAiToolsPayload(List.of("get_current_time"));
        }
        return agentToolRegistry.openAiToolsPayload();
    }

    private static boolean looksLikeWeatherQuestion(String q) {
        if (!StringUtils.hasText(q)) {
            return false;
        }
        String text = q.toLowerCase(Locale.ROOT);
        return text.contains("天气") || text.contains("气温") || text.contains("下雨")
                || text.contains("下雪") || text.contains("温度") || text.contains("forecast");
    }

    private static boolean looksLikeTimeQuestion(String q) {
        if (!StringUtils.hasText(q)) {
            return false;
        }
        String text = q;
        return text.contains("几点") || text.contains("几号") || text.contains("星期")
                || text.contains("日期") || text.contains("现在时间") || text.contains("当前时间")
                || text.contains("今天几号");
    }

    private static List<ChatCompletionsClient.ToolCall> orderToolCalls(
            List<ChatCompletionsClient.ToolCall> calls) {
        List<ChatCompletionsClient.ToolCall> ordered = new ArrayList<>(calls);
        ordered.sort((a, b) -> Integer.compare(toolPriority(a.name()), toolPriority(b.name())));
        return ordered;
    }

    private static int toolPriority(String name) {
        if ("get_current_time".equals(name)) {
            return 0;
        }
        if ("get_weather".equals(name)) {
            return 1;
        }
        if ("web_search".equals(name)) {
            return 2;
        }
        return 3;
    }

    private boolean hasNewExecutableCall(List<ChatCompletionsClient.ToolCall> calls,
                                         Map<String, String> resultByFingerprint,
                                         Map<String, String> resultByToolName,
                                         boolean weatherSucceeded) {
        Set<String> seenNames = new LinkedHashSet<>();
        for (ChatCompletionsClient.ToolCall call : calls) {
            String name = call.name() == null ? "" : call.name();
            if ("web_search".equals(name) && weatherSucceeded) {
                continue;
            }
            if (resultByFingerprint.containsKey(fingerprint(call)) || resultByToolName.containsKey(name)) {
                continue;
            }
            if (!seenNames.add(name)) {
                continue;
            }
            return true;
        }
        return false;
    }

    private String fingerprint(ChatCompletionsClient.ToolCall call) {
        String name = call.name() == null ? "" : call.name();
        String args = normalizeArgs(call.argumentsJson());
        // 天气/时间：同名即视为同一调用（忽略参数细微差别）
        if ("get_weather".equals(name) || "get_current_time".equals(name)) {
            return name;
        }
        return name + "|" + args;
    }

    private String normalizeArgs(String raw) {
        try {
            JsonNode node = objectMapper.readTree(StringUtils.hasText(raw) ? raw : "{}");
            return objectMapper.writeValueAsString(node);
        } catch (Exception ex) {
            return raw == null ? "{}" : raw.trim();
        }
    }

    private static boolean isToolSuccess(String result) {
        if (!StringUtils.hasText(result)) {
            return false;
        }
        String text = result.toLowerCase(Locale.ROOT);
        return !text.contains("失败")
                && !text.contains("缺少")
                && !text.contains("未找到")
                && !text.contains("未配置");
    }

    private String finalizeNaturalAnswer(LlmModel model,
                                         List<Map<String, Object>> messages,
                                         Consumer<String> onDelta) {
        List<Map<String, Object>> finalMessages = new ArrayList<>(messages);
        finalMessages.add(Map.of("role", "user", "content", FINALIZE_USER_PROMPT));

        String content = streamAndCollect(model, finalMessages, onDelta);
        if (StringUtils.hasText(content) && !looksLikeToolMarkup(content)) {
            return content;
        }

        finalMessages.add(Map.of("role", "assistant", "content", StringUtils.hasText(content) ? content : null));
        finalMessages.add(Map.of("role", "user", "content",
                "上一次输出无效。请立刻用中文给出最终回答，不要包含任何标签或工具语法。"));
        content = streamAndCollect(model, finalMessages, onDelta);
        if (StringUtils.hasText(content) && !looksLikeToolMarkup(content)) {
            return content;
        }

        if (looksLikeToolMarkup(content)) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR,
                    "模型在收尾阶段仍尝试调用工具（DeepSeek DSML）。请重试，或更换支持标准 tool_calls 的对话模型。");
        }
        throw new BusinessException(ResultCode.INTERNAL_ERROR, "智能体最终回答为空");
    }

    /**
     * 收尾阶段（无 tools）流式生成最终回答。
     */
    private String streamAndCollect(LlmModel model,
                                    List<Map<String, Object>> messages,
                                    Consumer<String> onDelta) {
        StringBuilder full = new StringBuilder();
        chatCompletionsClient.stream(model, messages, delta -> {
            full.append(delta);
            onDelta.accept(delta);
        });
        return safeText(full.toString());
    }

    private Map<String, Object> toAssistantToolCallMessage(ChatCompletionsClient.AssistantMessage turn) {
        Map<String, Object> assistantMsg = new LinkedHashMap<>();
        assistantMsg.put("role", "assistant");
        assistantMsg.put("content", StringUtils.hasText(turn.content()) ? turn.content() : null);
        List<Map<String, Object>> toolCallPayload = new ArrayList<>();
        for (ChatCompletionsClient.ToolCall call : turn.toolCalls()) {
            Map<String, Object> fn = new LinkedHashMap<>();
            fn.put("name", call.name());
            fn.put("arguments", call.argumentsJson() == null ? "{}" : call.argumentsJson());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", call.id());
            item.put("type", "function");
            item.put("function", fn);
            toolCallPayload.add(item);
        }
        assistantMsg.put("tool_calls", toolCallPayload);
        return assistantMsg;
    }

    private String executeTool(ChatCompletionsClient.ToolCall call) {
        AgentTool tool = agentToolRegistry.get(call.name());
        if (tool == null) {
            return "未知工具: " + call.name();
        }
        try {
            JsonNode args = objectMapper.readTree(
                    StringUtils.hasText(call.argumentsJson()) ? call.argumentsJson() : "{}");
            String result = tool.execute(args);
            log.info("Agent tool {} done, resultChars={}", call.name(),
                    result == null ? 0 : result.length());
            return result == null ? "" : result;
        } catch (Exception ex) {
            log.warn("Agent tool {} failed: {}", call.name(), ex.getMessage());
            return "工具执行失败（" + call.name() + "）: " + ex.getMessage();
        }
    }

    static boolean looksLikeToolMarkup(String text) {
        if (!StringUtils.hasText(text)) {
            return false;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        return lower.contains("｜dsml｜")
                || lower.contains("|dsml|")
                || lower.contains("dsml｜")
                || lower.contains("<tool_calls>")
                || lower.contains("tool_calls>")
                || lower.contains("invoke name=")
                || lower.contains("<｜tool");
    }

    private static String safeText(String content) {
        return content == null ? "" : content.trim();
    }

    private static String toolDisplayName(String name) {
        return switch (name) {
            case "get_current_time" -> "正在查询当前时间…";
            case "get_weather" -> "正在查询天气…";
            case "web_search" -> "正在搜索网页…";
            default -> "正在调用工具 " + name + "…";
        };
    }
}
