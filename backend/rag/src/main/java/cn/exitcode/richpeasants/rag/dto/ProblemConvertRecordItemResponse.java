package cn.exitcode.richpeasants.rag.dto;

import java.time.LocalDateTime;

public class ProblemConvertRecordItemResponse {
    private Long id;
    private String title;
    private String referenceNickname;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ProblemConvertRecordItemResponse() {
    }

    public ProblemConvertRecordItemResponse(Long id,
                                            String title,
                                            String referenceNickname,
                                            LocalDateTime createdAt,
                                            LocalDateTime updatedAt) {
        this.id = id;
        this.title = title;
        this.referenceNickname = referenceNickname;
        this.createdAt = createdAt;
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

    public String getReferenceNickname() {
        return referenceNickname;
    }

    public void setReferenceNickname(String referenceNickname) {
        this.referenceNickname = referenceNickname;
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
