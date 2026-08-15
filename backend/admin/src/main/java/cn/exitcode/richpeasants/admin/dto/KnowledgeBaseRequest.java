package cn.exitcode.richpeasants.admin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class KnowledgeBaseRequest {

    @NotBlank(message = "知识库名称不能为空")
    @Size(max = 128, message = "知识库名称不能超过128字符")
    private String name;

    @Size(max = 512, message = "描述不能超过512字符")
    private String description;

    private Boolean enabled = true;

    /** 留空/null = 使用系统默认 */
    @Min(value = 100, message = "切分长度至少 100")
    @Max(value = 8000, message = "切分长度不能超过 8000")
    private Integer defaultChunkSize;

    /** 留空/null = 使用系统默认 */
    @Min(value = 0, message = "重叠长度不能为负")
    @Max(value = 4000, message = "重叠长度不能超过 4000")
    private Integer defaultChunkOverlap;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Integer getDefaultChunkSize() {
        return defaultChunkSize;
    }

    public void setDefaultChunkSize(Integer defaultChunkSize) {
        this.defaultChunkSize = defaultChunkSize;
    }

    public Integer getDefaultChunkOverlap() {
        return defaultChunkOverlap;
    }

    public void setDefaultChunkOverlap(Integer defaultChunkOverlap) {
        this.defaultChunkOverlap = defaultChunkOverlap;
    }
}
