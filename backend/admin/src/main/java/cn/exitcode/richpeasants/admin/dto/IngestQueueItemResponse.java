package cn.exitcode.richpeasants.admin.dto;

import cn.exitcode.richpeasants.common.enums.DocumentStatus;

import java.time.LocalDateTime;

public class IngestQueueItemResponse {

    private Long id;
    private Long kbId;
    private String title;
    private DocumentStatus status;
    private Integer chunkCount;
    private Integer parsedCharCount;
    private String errorMessage;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getKbId() {
        return kbId;
    }

    public void setKbId(Long kbId) {
        this.kbId = kbId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public DocumentStatus getStatus() {
        return status;
    }

    public void setStatus(DocumentStatus status) {
        this.status = status;
    }

    public Integer getChunkCount() {
        return chunkCount;
    }

    public void setChunkCount(Integer chunkCount) {
        this.chunkCount = chunkCount;
    }

    public Integer getParsedCharCount() {
        return parsedCharCount;
    }

    public void setParsedCharCount(Integer parsedCharCount) {
        this.parsedCharCount = parsedCharCount;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
