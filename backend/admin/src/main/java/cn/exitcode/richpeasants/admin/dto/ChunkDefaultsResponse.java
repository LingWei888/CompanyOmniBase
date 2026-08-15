package cn.exitcode.richpeasants.admin.dto;

public class ChunkDefaultsResponse {

    /** 最终生效值（文档不填时会用到） */
    private Integer chunkSize;
    private Integer chunkOverlap;

    private Integer systemChunkSize;
    private Integer systemChunkOverlap;

    /** 知识库自身配置；null 表示未单独设置 */
    private Integer kbChunkSize;
    private Integer kbChunkOverlap;

    public ChunkDefaultsResponse() {
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

    public Integer getSystemChunkSize() {
        return systemChunkSize;
    }

    public void setSystemChunkSize(Integer systemChunkSize) {
        this.systemChunkSize = systemChunkSize;
    }

    public Integer getSystemChunkOverlap() {
        return systemChunkOverlap;
    }

    public void setSystemChunkOverlap(Integer systemChunkOverlap) {
        this.systemChunkOverlap = systemChunkOverlap;
    }

    public Integer getKbChunkSize() {
        return kbChunkSize;
    }

    public void setKbChunkSize(Integer kbChunkSize) {
        this.kbChunkSize = kbChunkSize;
    }

    public Integer getKbChunkOverlap() {
        return kbChunkOverlap;
    }

    public void setKbChunkOverlap(Integer kbChunkOverlap) {
        this.kbChunkOverlap = kbChunkOverlap;
    }
}
