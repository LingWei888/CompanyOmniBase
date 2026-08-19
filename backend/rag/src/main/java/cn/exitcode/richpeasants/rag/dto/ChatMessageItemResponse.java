package cn.exitcode.richpeasants.rag.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ChatMessageItemResponse {

    private Long id;
    private String role;
    private String content;
    private List<RagCitation> citations = new ArrayList<>();
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<RagCitation> getCitations() {
        return citations;
    }

    public void setCitations(List<RagCitation> citations) {
        this.citations = citations == null ? new ArrayList<>() : citations;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
