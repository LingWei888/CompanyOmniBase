package cn.exitcode.richpeasants.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "problem_convert_record")
public class ProblemConvertRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 200)
    private String title = "未命名转换";

    @Column(name = "reference_nickname", nullable = false, length = 200)
    private String referenceNickname = "";

    @Lob
    @Column(name = "original_text", nullable = false, columnDefinition = "LONGTEXT")
    private String originalText = "";

    @Lob
    @Column(name = "result_markdown", nullable = false, columnDefinition = "LONGTEXT")
    private String resultMarkdown = "";

    @Lob
    @Column(name = "solution_code", columnDefinition = "LONGTEXT")
    private String solutionCode = "";

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.title == null || this.title.isBlank()) {
            this.title = "未命名转换";
        }
        if (this.referenceNickname == null) {
            this.referenceNickname = "";
        }
        if (this.originalText == null) {
            this.originalText = "";
        }
        if (this.resultMarkdown == null) {
            this.resultMarkdown = "";
        }
        if (this.solutionCode == null) {
            this.solutionCode = "";
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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
