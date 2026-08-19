package cn.exitcode.richpeasants.rag.agent;

import cn.exitcode.richpeasants.rag.config.RagAppProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class WeatherTool implements AgentTool {

    private final RestClient agentRestClient;
    private final RagAppProperties ragAppProperties;
    private final ObjectMapper objectMapper;

    public WeatherTool(@Qualifier("agentRestClient") RestClient agentRestClient,
                       RagAppProperties ragAppProperties,
                       ObjectMapper objectMapper) {
        this.agentRestClient = agentRestClient;
        this.ragAppProperties = ragAppProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    public String name() {
        return "get_weather";
    }

    @Override
    public String description() {
        return "查询指定城市的当前天气（气温、湿度、风速、天气状况）。询问天气时必须调用本工具。";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        Map<String, Object> city = new LinkedHashMap<>();
        city.put("type", "string");
        city.put("description", "城市名称，如 杭州、北京、Shanghai");

        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("city", city);

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", java.util.List.of("city"));
        schema.put("additionalProperties", false);
        return schema;
    }

    @Override
    public String execute(JsonNode arguments) throws Exception {
        String city = arguments == null ? "" : arguments.path("city").asText("").trim();
        if (!StringUtils.hasText(city)) {
            return "缺少城市参数 city";
        }

        String geocodeUrl = ragAppProperties.getAgent().getWeather().getGeocodeUrl();
        URI geoUri = UriComponentsBuilder.fromUriString(geocodeUrl)
                .queryParam("name", city)
                .queryParam("count", 1)
                .queryParam("language", "zh")
                .queryParam("format", "json")
                .build()
                .encode()
                .toUri();

        String geoJson = agentRestClient.get()
                .uri(geoUri)
                .retrieve()
                .body(String.class);
        JsonNode geoRoot = objectMapper.readTree(geoJson == null ? "{}" : geoJson);
        JsonNode results = geoRoot.path("results");
        if (!results.isArray() || results.isEmpty()) {
            return "未找到城市：" + city;
        }
        JsonNode place = results.get(0);
        double lat = place.path("latitude").asDouble();
        double lon = place.path("longitude").asDouble();
        String name = place.path("name").asText(city);
        String country = place.path("country").asText("");
        String admin = place.path("admin1").asText("");

        String forecastUrl = ragAppProperties.getAgent().getWeather().getForecastUrl();
        URI forecastUri = UriComponentsBuilder.fromUriString(forecastUrl)
                .queryParam("latitude", lat)
                .queryParam("longitude", lon)
                .queryParam("current", "temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m")
                .queryParam("timezone", "auto")
                .build()
                .encode()
                .toUri();

        String weatherJson = agentRestClient.get()
                .uri(forecastUri)
                .retrieve()
                .body(String.class);
        JsonNode weatherRoot = objectMapper.readTree(weatherJson == null ? "{}" : weatherJson);
        JsonNode current = weatherRoot.path("current");
        if (current.isMissingNode() || current.isNull()) {
            return "已定位到 " + name + "，但未能获取天气数据";
        }

        double temp = current.path("temperature_2m").asDouble(Double.NaN);
        int humidity = current.path("relative_humidity_2m").asInt(-1);
        int code = current.path("weather_code").asInt(-1);
        double wind = current.path("wind_speed_10m").asDouble(Double.NaN);
        String time = current.path("time").asText("");

        StringBuilder sb = new StringBuilder();
        sb.append("地点：").append(name);
        if (StringUtils.hasText(admin)) {
            sb.append("，").append(admin);
        }
        if (StringUtils.hasText(country)) {
            sb.append("，").append(country);
        }
        sb.append('\n');
        if (StringUtils.hasText(time)) {
            sb.append("观测时间：").append(time).append('\n');
        }
        if (!Double.isNaN(temp)) {
            sb.append("气温：").append(temp).append("°C\n");
        }
        sb.append("天气：").append(weatherCodeLabel(code)).append('\n');
        if (humidity >= 0) {
            sb.append("湿度：").append(humidity).append("%\n");
        }
        if (!Double.isNaN(wind)) {
            sb.append("风速：").append(wind).append(" km/h");
        }
        return sb.toString().trim();
    }

    private static String weatherCodeLabel(int code) {
        return switch (code) {
            case 0 -> "晴";
            case 1, 2, 3 -> "多云";
            case 45, 48 -> "雾";
            case 51, 53, 55 -> "毛毛雨";
            case 61, 63, 65 -> "雨";
            case 66, 67 -> "冻雨";
            case 71, 73, 75, 77 -> "雪";
            case 80, 81, 82 -> "阵雨";
            case 85, 86 -> "阵雪";
            case 95 -> "雷雨";
            case 96, 99 -> "雷暴伴冰雹";
            default -> code < 0 ? "未知" : ("天气代码 " + code);
        };
    }
}
