package cn.exitcode.richpeasants.rag.dto;

import java.util.ArrayList;
import java.util.List;

public class RagAskResponse {

    private String answer;
    private List<Long> kbIds = new ArrayList<>();
    private List<String> kbNames = new ArrayList<>();
    /** 兼容旧前端：取第一个知识库 */
    private Long kbId;
    private String kbName;
    private Long modelId;
    private String modelName;
    private List<RagCitation> citations = new ArrayList<>();

    public RagAskResponse() {
    }

    public RagAskResponse(String answer,
                          List<Long> kbIds,
                          List<String> kbNames,
                          Long modelId,
                          String modelName,
                          List<RagCitation> citations) {
        this.answer = answer;
        this.kbIds = kbIds == null ? new ArrayList<>() : kbIds;
        this.kbNames = kbNames == null ? new ArrayList<>() : kbNames;
        this.kbId = this.kbIds.isEmpty() ? null : this.kbIds.get(0);
        this.kbName = this.kbNames.isEmpty() ? "全部知识库" : String.join("、", this.kbNames);
        this.modelId = modelId;
        this.modelName = modelName;
        this.citations = citations == null ? new ArrayList<>() : citations;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public List<Long> getKbIds() {
        return kbIds;
    }

    public void setKbIds(List<Long> kbIds) {
        this.kbIds = kbIds;
    }

    public List<String> getKbNames() {
        return kbNames;
    }

    public void setKbNames(List<String> kbNames) {
        this.kbNames = kbNames;
    }

    public Long getKbId() {
        return kbId;
    }

    public void setKbId(Long kbId) {
        this.kbId = kbId;
    }

    public String getKbName() {
        return kbName;
    }

    public void setKbName(String kbName) {
        this.kbName = kbName;
    }

    public Long getModelId() {
        return modelId;
    }

    public void setModelId(Long modelId) {
        this.modelId = modelId;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public List<RagCitation> getCitations() {
        return citations;
    }

    public void setCitations(List<RagCitation> citations) {
        this.citations = citations;
    }
}
