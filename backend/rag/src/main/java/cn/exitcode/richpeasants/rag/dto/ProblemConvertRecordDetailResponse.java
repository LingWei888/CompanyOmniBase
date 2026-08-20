package cn.exitcode.richpeasants.rag.dto;

import java.time.LocalDateTime;

public class ProblemConvertRecordDetailResponse {
    private Long id;
    private String title;
    private String referenceNickname;
    private String originalText;
    private String resultMarkdown;
    private String solutionCode;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
