package cn.exitcode.richpeasants.rag.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ProblemConvertRequest {

    @NotNull(message = "对话模型不能为空")
    private Long modelId;

    @NotBlank(message = "目标标题不能为空")
    @Size(max = 200, message = "目标标题过长")
    private String referenceNickname;

    @NotBlank(message = "原题内容不能为空")
    @Size(max = 12000, message = "原题内容过长")
    private String originalText;

    /** 可选：题解/标程代码，用于约束换皮不跑偏 */
    @Size(max = 80000, message = "题解代码过长")
    private String solutionCode;

    public Long getModelId() {
        return modelId;
    }

    public void setModelId(Long modelId) {
        this.modelId = modelId;
    }

    public String getReferenceNickname() {
        return referenceNickname;
    }

    public void setReferenceNickname(String referenceNickname) {
        this.referenceNickname = referenceNickname;
    }

    public String getOriginalText() {
        return originalText;
    }

    public void setOriginalText(String originalText) {
        this.originalText = originalText;
    }

    public String getSolutionCode() {
        return solutionCode;
    }

    public void setSolutionCode(String solutionCode) {
        this.solutionCode = solutionCode;
    }
}
