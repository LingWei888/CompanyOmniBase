package cn.exitcode.richpeasants.rag.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

public class RagAskRequest {

    /**
     * 知识库 ID 列表；空或未传表示不启用 RAG（纯对话）。
     * 兼容旧字段 kbId：若 kbIds 为空且 kbId 有值，则按单库处理。
     */
    private List<Long> kbIds = new ArrayList<>();

    /** @deprecated 使用 kbIds；保留兼容 */
    private Long kbId;

    @NotNull(message = "对话模型不能为空")
    private Long modelId;

    @NotBlank(message = "问题不能为空")
    @Size(max = 4000, message = "问题过长")
    private String question;

    @Min(value = 1, message = "topK 至少为 1")
    @Max(value = 20, message = "topK 不能超过 20")
    private Integer topK;

    public List<Long> getKbIds() {
        return kbIds;
    }

    public void setKbIds(List<Long> kbIds) {
        this.kbIds = kbIds == null ? new ArrayList<>() : kbIds;
    }

    public Long getKbId() {
        return kbId;
    }

    public void setKbId(Long kbId) {
        this.kbId = kbId;
    }

    public Long getModelId() {
        return modelId;
    }

    public void setModelId(Long modelId) {
        this.modelId = modelId;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public Integer getTopK() {
        return topK;
    }

    public void setTopK(Integer topK) {
        this.topK = topK;
    }
}
