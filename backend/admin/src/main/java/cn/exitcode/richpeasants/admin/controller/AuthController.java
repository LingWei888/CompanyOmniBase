package cn.exitcode.richpeasants.admin.controller;

import cn.exitcode.richpeasants.admin.dto.LoginRequest;
import cn.exitcode.richpeasants.admin.dto.LoginResponse;
import cn.exitcode.richpeasants.admin.dto.RefreshTokenRequest;
import cn.exitcode.richpeasants.admin.service.AuthService;
import cn.exitcode.richpeasants.common.result.ApiResult;
import cn.exitcode.richpeasants.common.security.LoginUser;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResult<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResult.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ApiResult<LoginResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResult.ok(authService.refresh(request.getRefreshToken()));
    }

    @PostMapping("/logout")
    public ApiResult<Void> logout() {
        // JWT 无状态，前端清除本地 Token 即可；后续可接黑名单
        return ApiResult.ok();
    }

    @GetMapping("/me")
    public ApiResult<LoginResponse.UserInfo> me(@AuthenticationPrincipal LoginUser loginUser) {
        return ApiResult.ok(authService.currentUser(loginUser));
    }
}
