package cn.exitcode.richpeasants.admin.dto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IngestOpsOverviewResponse {

    private Map<String, Long> documentStatusCounts = new HashMap<>();
    private long waitingEmbeddingCount;
    private long embeddingCount;
    private long failedCount;
    private long readyCount;
    private List<IngestQueueItemResponse> waitingEmbedding = new ArrayList<>();
    private List<IngestQueueItemResponse> embedding = new ArrayList<>();

    public Map<String, Long> getDocumentStatusCounts() {
        return documentStatusCounts;
    }

    public void setDocumentStatusCounts(Map<String, Long> documentStatusCounts) {
        this.documentStatusCounts = documentStatusCounts;
    }

    public long getWaitingEmbeddingCount() {
        return waitingEmbeddingCount;
    }

    public void setWaitingEmbeddingCount(long waitingEmbeddingCount) {
        this.waitingEmbeddingCount = waitingEmbeddingCount;
    }

    public long getEmbeddingCount() {
        return embeddingCount;
    }

    public void setEmbeddingCount(long embeddingCount) {
        this.embeddingCount = embeddingCount;
    }

    public long getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(long failedCount) {
        this.failedCount = failedCount;
    }

    public long getReadyCount() {
        return readyCount;
    }

    public void setReadyCount(long readyCount) {
        this.readyCount = readyCount;
    }

    public List<IngestQueueItemResponse> getWaitingEmbedding() {
        return waitingEmbedding;
    }

    public void setWaitingEmbedding(List<IngestQueueItemResponse> waitingEmbedding) {
        this.waitingEmbedding = waitingEmbedding;
    }

    public List<IngestQueueItemResponse> getEmbedding() {
        return embedding;
    }

    public void setEmbedding(List<IngestQueueItemResponse> embedding) {
        this.embedding = embedding;
    }
}
