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
}
