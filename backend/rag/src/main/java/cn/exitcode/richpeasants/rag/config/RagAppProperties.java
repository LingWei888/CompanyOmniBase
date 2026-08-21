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
    private final Memory memory = new Memory();

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

    public Memory getMemory() {
        return memory;
    }

    public static class Memory {
        private final ShortTerm shortTerm = new ShortTerm();
        private final LongTerm longTerm = new LongTerm();

        public ShortTerm getShortTerm() {
            return shortTerm;
        }

        public LongTerm getLongTerm() {
            return longTerm;
        }
    }

    public static class ShortTerm {
        /** 注入模型的最近对话轮数（一轮 = user + assistant） */
        private int maxTurns = 8;
        /** 历史正文总字符上限，超出则从最旧轮次截断 */
        private int maxChars = 12000;

        public int getMaxTurns() {
            return maxTurns;
        }

        public void setMaxTurns(int maxTurns) {
            this.maxTurns = maxTurns;
        }

        public int getMaxChars() {
            return maxChars;
        }

        public void setMaxChars(int maxChars) {
            this.maxChars = maxChars;
        }
    }

    public static class LongTerm {
        private boolean enabled = true;
        /** 每次问答检索条数 */
        private int topK = 5;
        /** 低于该相关度不注入（cosine score，约 0~1） */
        private double minScore = 0.52;
        /** 新事实与已有记忆相似度高于此值则跳过（去重） */
        private double duplicateThreshold = 0.90;
        /** 注入 system 的记忆总字符上限 */
        private int maxInjectChars = 1500;
        /** 单用户最多保留条数，超出删最旧 */
        private int maxPerUser = 100;
        /** 是否在回答后异步抽取并写入 */
        private boolean extractEnabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getTopK() {
            return topK;
        }

        public void setTopK(int topK) {
            this.topK = topK;
        }

        public double getMinScore() {
            return minScore;
        }

        public void setMinScore(double minScore) {
            this.minScore = minScore;
        }

        public double getDuplicateThreshold() {
            return duplicateThreshold;
        }

        public void setDuplicateThreshold(double duplicateThreshold) {
            this.duplicateThreshold = duplicateThreshold;
        }

        public int getMaxInjectChars() {
            return maxInjectChars;
        }

        public void setMaxInjectChars(int maxInjectChars) {
            this.maxInjectChars = maxInjectChars;
        }

        public int getMaxPerUser() {
            return maxPerUser;
        }

        public void setMaxPerUser(int maxPerUser) {
            this.maxPerUser = maxPerUser;
        }

        public boolean isExtractEnabled() {
            return extractEnabled;
        }

        public void setExtractEnabled(boolean extractEnabled) {
            this.extractEnabled = extractEnabled;
        }
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
