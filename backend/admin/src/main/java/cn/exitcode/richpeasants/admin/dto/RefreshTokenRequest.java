package cn.exitcode.richpeasants.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RefreshTokenRequest {

    @NotBlank(message = "refreshToken 不能为空")
    private String refreshToken;
}
