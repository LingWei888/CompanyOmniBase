package cn.exitcode.richpeasants.admin.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.ArrayList;
import java.util.List;

public class StartEmbeddingBatchRequest {

    @NotEmpty(message = "请至少选择一个文档")
    private List<Long> documentIds = new ArrayList<>();

    public List<Long> getDocumentIds() {
        return documentIds;
    }

    public void setDocumentIds(List<Long> documentIds) {
        this.documentIds = documentIds == null ? new ArrayList<>() : documentIds;
    }
}
