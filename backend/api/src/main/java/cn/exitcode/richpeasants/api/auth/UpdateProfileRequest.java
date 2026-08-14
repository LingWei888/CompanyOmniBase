package cn.exitcode.richpeasants.api.auth;

import jakarta.validation.constraints.Size;

public class UpdateProfileRequest {

    @Size(max = 64, message = "昵称过长")
    private String nickname;

    @Size(max = 512, message = "头像地址过长")
    private String avatarUrl;

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }
}
