package cn.exitcode.richpeasants.rag.agent;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class CurrentTimeTool implements AgentTool {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss EEEE", Locale.CHINA);

    @Override
    public String name() {
        return "get_current_time";
    }

    @Override
    public String description() {
        return "获取当前日期与时间。询问现在几点、今天几号、星期几时必须调用本工具，不要猜测。";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        Map<String, Object> timezone = new LinkedHashMap<>();
        timezone.put("type", "string");
        timezone.put("description", "IANA 时区，默认 Asia/Shanghai，例如 Asia/Shanghai、UTC");

        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("timezone", timezone);

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("additionalProperties", false);
        return schema;
    }

    @Override
    public String execute(JsonNode arguments) {
        String zone = "Asia/Shanghai";
        if (arguments != null && arguments.hasNonNull("timezone")) {
            String raw = arguments.get("timezone").asText("").trim();
            if (StringUtils.hasText(raw)) {
                zone = raw;
            }
        }
        try {
            ZoneId zoneId = ZoneId.of(zone);
            ZonedDateTime now = ZonedDateTime.now(zoneId);
            return "当前时间：" + now.format(FORMATTER) + "（时区 " + zoneId + "）";
        } catch (DateTimeException ex) {
            return "无效时区: " + zone + "。请使用如 Asia/Shanghai。";
        }
    }
}
