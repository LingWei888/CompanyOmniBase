package cn.exitcode.richpeasants.ingest.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class IngestAppProperties {

    private final Elasticsearch elasticsearch = new Elasticsearch();
    private final Embedding embedding = new Embedding();

    public Elasticsearch getElasticsearch() {
        return elasticsearch;
    }

    public Embedding getEmbedding() {
        return embedding;
    }

    public static class Elasticsearch {
        private String chunkIndex = "kb_chunk_vector";
        private String memoryIndex = "user_memory_vector";

        public String getChunkIndex() {
            return chunkIndex;
        }

        public void setChunkIndex(String chunkIndex) {
            this.chunkIndex = chunkIndex;
        }

        public String getMemoryIndex() {
            return memoryIndex;
        }

        public void setMemoryIndex(String memoryIndex) {
            this.memoryIndex = memoryIndex;
        }
    }

    public static class Embedding {
        /** 单次请求片段数；过大易触发上游慢/超时 */
        private int batchSize = 8;
        /** 连接超时（毫秒） */
        private int connectTimeoutMs = 15000;
        /** 读超时（毫秒）；硅基流动等网关偶发排队，默认给足 2 分钟 */
        private int readTimeoutMs = 120000;

        public int getBatchSize() {
            return batchSize;
        }

        public void setBatchSize(int batchSize) {
            this.batchSize = batchSize;
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
    }
}
