package cn.exitcode.richpeasants.rag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.rag")
public class RagAppProperties {

    /** 默认检索条数 */
    private int topK = 5;
    /** 拼进 Prompt 的上下文总字符上限 */
    private int maxContextChars = 6000;
    /** 返回给前端的引用正文截断长度 */
    private int citationPreviewChars = 240;
    private int connectTimeoutMs = 15000;
    private int readTimeoutMs = 120000;
    private double temperature = 0.2;
    private final Agent agent = new Agent();

    public int getTopK() {
        return topK;
    }

    public void setTopK(int topK) {
        this.topK = topK;
    }

    public int getMaxContextChars() {
        return maxContextChars;
    }

    public void setMaxContextChars(int maxContextChars) {
        this.maxContextChars = maxContextChars;
    }

    public int getCitationPreviewChars() {
        return citationPreviewChars;
    }

    public void setCitationPreviewChars(int citationPreviewChars) {
        this.citationPreviewChars = citationPreviewChars;
    }

    public int getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(int connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public int getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public void setReadTimeoutMs(int readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public Agent getAgent() {
        return agent;
    }

    public static class Agent {
        /** 工具调用最大轮数 */
        private int maxToolRounds = 5;
        private final Tavily tavily = new Tavily();
        private final Weather weather = new Weather();

        public int getMaxToolRounds() {
            return maxToolRounds;
        }

        public void setMaxToolRounds(int maxToolRounds) {
            this.maxToolRounds = maxToolRounds;
        }

        public Tavily getTavily() {
            return tavily;
        }

        public Weather getWeather() {
            return weather;
        }
    }

    public static class Tavily {
        private String apiKey = "";
        private String baseUrl = "https://api.tavily.com";
        private int maxResults = 5;

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public int getMaxResults() {
            return maxResults;
        }

        public void setMaxResults(int maxResults) {
            this.maxResults = maxResults;
        }
    }

    public static class Weather {
        private String geocodeUrl = "https://geocoding-api.open-meteo.com/v1/search";
        private String forecastUrl = "https://api.open-meteo.com/v1/forecast";

        public String getGeocodeUrl() {
            return geocodeUrl;
        }

        public void setGeocodeUrl(String geocodeUrl) {
            this.geocodeUrl = geocodeUrl;
        }

        public String getForecastUrl() {
            return forecastUrl;
        }

        public void setForecastUrl(String forecastUrl) {
            this.forecastUrl = forecastUrl;
        }
    }
}
