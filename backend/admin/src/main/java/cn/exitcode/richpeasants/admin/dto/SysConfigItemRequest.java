package cn.exitcode.richpeasants.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SysConfigItemRequest {

    @NotBlank(message = "配置键不能为空")
    @Size(max = 128, message = "配置键不能超过128字符")
    private String configKey;

    private String configValue;

    @Size(max = 256, message = "备注不能超过256字符")
    private String remark;

    public String getConfigKey() {
        return configKey;
    }

    public void setConfigKey(String configKey) {
        this.configKey = configKey;
    }

    public String getConfigValue() {
        return configValue;
    }

    public void setConfigValue(String configValue) {
        this.configValue = configValue;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
