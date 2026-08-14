package cn.exitcode.richpeasants.admin.dto;

import cn.exitcode.richpeasants.common.enums.LlmProtocol;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class LlmModelRequest {

    @NotBlank(message = "模型名称不能为空")
    @Size(max = 128, message = "模型名称不能超过128字符")
    private String name;

    @NotNull(message = "对接方式不能为空")
    private LlmProtocol protocol = LlmProtocol.OPENAI;

    @NotBlank(message = "API Base URL 不能为空")
    @Size(max = 512, message = "URL 不能超过512字符")
    private String baseUrl;

    @NotBlank(message = "API Key 不能为空")
    @Size(max = 512, message = "API Key 不能超过512字符")
    private String apiKey;

    @Size(max = 128, message = "模型标识不能超过128字符")
    private String modelName;

    private Boolean enabled = true;

    @Size(max = 256, message = "备注不能超过256字符")
    private String remark;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LlmProtocol getProtocol() {
        return protocol;
    }

    public void setProtocol(LlmProtocol protocol) {
        this.protocol = protocol;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
