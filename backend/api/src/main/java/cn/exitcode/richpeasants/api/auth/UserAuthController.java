package cn.exitcode.richpeasants.api.auth;

import cn.exitcode.richpeasants.common.result.ApiResult;
import cn.exitcode.richpeasants.common.result.ResultCode;
import cn.exitcode.richpeasants.common.security.LoginUser;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class UserAuthController {

    private final UserAuthService userAuthService;

    public UserAuthController(UserAuthService userAuthService) {
        this.userAuthService = userAuthService;
    }

    @PostMapping("/register")
    public ApiResult<UserAuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResult.ok(userAuthService.register(request));
    }

    @PostMapping("/login")
    public ApiResult<UserAuthResponse> login(@Valid @RequestBody UserLoginRequest request) {
        return ApiResult.ok(userAuthService.login(request));
    }

    @PostMapping("/refresh")
    public ApiResult<UserAuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResult.ok(userAuthService.refresh(request.getRefreshToken()));
    }

    @PostMapping("/logout")
    public ApiResult<Void> logout() {
        return ApiResult.ok();
    }

    @GetMapping("/me")
    public ApiResult<UserAuthResponse.UserInfo> me(@AuthenticationPrincipal LoginUser loginUser) {
        if (loginUser == null) {
            return ApiResult.fail(ResultCode.UNAUTHORIZED);
        }
        return ApiResult.ok(userAuthService.currentUser(loginUser));
    }

    @PutMapping("/profile")
    public ApiResult<UserAuthResponse.UserInfo> updateProfile(@AuthenticationPrincipal LoginUser loginUser,
                                                              @Valid @RequestBody UpdateProfileRequest request) {
        if (loginUser == null) {
            return ApiResult.fail(ResultCode.UNAUTHORIZED);
        }
        return ApiResult.ok(userAuthService.updateProfile(loginUser, request));
    }

    @PutMapping("/password")
    public ApiResult<Void> changePassword(@AuthenticationPrincipal LoginUser loginUser,
                                          @Valid @RequestBody ChangePasswordRequest request) {
        if (loginUser == null) {
            return ApiResult.fail(ResultCode.UNAUTHORIZED);
        }
        userAuthService.changePassword(loginUser, request);
        return ApiResult.ok();
    }
}
