package cn.exitcode.richpeasants.admin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class DocumentUpdateRequest {

    @NotBlank(message = "文档标题不能为空")
    @Size(max = 256, message = "标题不能超过256字符")
    private String title;

    @Min(value = 100, message = "切分长度至少 100")
    @Max(value = 8000, message = "切分长度不能超过 8000")
    private Integer chunkSize;

    @Min(value = 0, message = "重叠长度不能为负")
    @Max(value = 4000, message = "重叠长度不能超过 4000")
    private Integer chunkOverlap;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(Integer chunkSize) {
        this.chunkSize = chunkSize;
    }

    public Integer getChunkOverlap() {
        return chunkOverlap;
    }

    public void setChunkOverlap(Integer chunkOverlap) {
        this.chunkOverlap = chunkOverlap;
    }
}
