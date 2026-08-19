package cn.exitcode.richpeasants.rag.dto;

import java.util.ArrayList;
import java.util.List;

public class ChatSessionDetailResponse {

    private Long id;
    private String title;
    private Long modelId;
    private List<Long> kbIds = new ArrayList<>();
    private List<ChatMessageItemResponse> messages = new ArrayList<>();

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

    public List<ChatMessageItemResponse> getMessages() {
        return messages;
    }

    public void setMessages(List<ChatMessageItemResponse> messages) {
        this.messages = messages;
    }
}
