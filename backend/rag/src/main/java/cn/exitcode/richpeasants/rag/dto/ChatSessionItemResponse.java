package cn.exitcode.richpeasants.rag.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ChatSessionItemResponse {

    private Long id;
    private String title;
    private Long modelId;
    private List<Long> kbIds = new ArrayList<>();
    private LocalDateTime updatedAt;

    public ChatSessionItemResponse() {
    }

    public ChatSessionItemResponse(Long id, String title, Long modelId, List<Long> kbIds, LocalDateTime updatedAt) {
        this.id = id;
        this.title = title;
        this.modelId = modelId;
        this.kbIds = kbIds == null ? new ArrayList<>() : kbIds;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Long getModelId() {
        return modelId;
    }

    public void setModelId(Long modelId) {
        this.modelId = modelId;
    }

    public List<Long> getKbIds() {
        return kbIds;
    }

    public void setKbIds(List<Long> kbIds) {
        this.kbIds = kbIds;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
