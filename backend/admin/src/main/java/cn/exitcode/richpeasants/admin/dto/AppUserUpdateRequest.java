package cn.exitcode.richpeasants.admin.dto;

import cn.exitcode.richpeasants.common.enums.UserPlan;
import jakarta.validation.constraints.Size;

public class AppUserUpdateRequest {

    @Size(max = 64, message = "昵称过长")
    private String nickname;

    private UserPlan plan;

    private Boolean enabled;

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public UserPlan getPlan() {
        return plan;
    }

    public void setPlan(UserPlan plan) {
        this.plan = plan;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}
