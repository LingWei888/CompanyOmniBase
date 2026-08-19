package cn.exitcode.richpeasants.rag.dto;

import jakarta.validation.constraints.Size;

import java.util.List;

public class ChatSessionUpdateRequest {

    @Size(max = 200, message = "标题过长")
    private String title;

    private List<Long> kbIds;

    private Long modelId;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<Long> getKbIds() {
        return kbIds;
    }

    public void setKbIds(List<Long> kbIds) {
        this.kbIds = kbIds;
    }

    public Long getModelId() {
        return modelId;
    }

    public void setModelId(Long modelId) {
        this.modelId = modelId;
    }
}
