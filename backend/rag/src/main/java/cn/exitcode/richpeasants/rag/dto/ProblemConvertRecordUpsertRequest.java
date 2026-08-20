package cn.exitcode.richpeasants.rag.dto;

import jakarta.validation.constraints.Size;

public class ProblemConvertRecordUpsertRequest {

    @Size(max = 200)
    private String title;

    @Size(max = 200)
    private String referenceNickname;

    private String originalText;

    private String resultMarkdown;

    @Size(max = 80000)
    private String solutionCode;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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

    public String getResultMarkdown() {
        return resultMarkdown;
    }

    public void setResultMarkdown(String resultMarkdown) {
        this.resultMarkdown = resultMarkdown;
    }

    public String getSolutionCode() {
        return solutionCode;
    }

    public void setSolutionCode(String solutionCode) {
        this.solutionCode = solutionCode;
    }
}
