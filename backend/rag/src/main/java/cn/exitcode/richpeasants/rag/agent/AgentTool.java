package cn.exitcode.richpeasants.rag.agent;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

/**
 * Agent 可调用工具。
 */
public interface AgentTool {

    String name();

    String description();

    /** JSON Schema 风格的 parameters 对象（供 OpenAI tools 使用） */
    Map<String, Object> parametersSchema();

    String execute(JsonNode arguments) throws Exception;
}
