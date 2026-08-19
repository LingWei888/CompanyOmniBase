package cn.exitcode.richpeasants.rag.agent;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class AgentToolRegistry {

    private final Map<String, AgentTool> tools = new LinkedHashMap<>();

    public AgentToolRegistry(List<AgentTool> toolList) {
        for (AgentTool tool : toolList) {
            tools.put(tool.name(), tool);
        }
    }

    public AgentTool get(String name) {
        return tools.get(name);
    }

    public Collection<AgentTool> all() {
        return tools.values();
    }

    /**
     * OpenAI 兼容 tools 数组（全部）。
     */
    public List<Map<String, Object>> openAiToolsPayload() {
        return openAiToolsPayload(tools.keySet().stream().toList());
    }

    /**
     * 仅暴露指定工具，用于按问题类型收窄模型选择面。
     */
    public List<Map<String, Object>> openAiToolsPayload(List<String> names) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (String name : names) {
            AgentTool tool = tools.get(name);
            if (tool == null) {
                continue;
            }
            Map<String, Object> fn = new LinkedHashMap<>();
            fn.put("name", tool.name());
            fn.put("description", tool.description());
            fn.put("parameters", tool.parametersSchema());

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("type", "function");
            item.put("function", fn);
            list.add(item);
        }
        return list;
    }
}