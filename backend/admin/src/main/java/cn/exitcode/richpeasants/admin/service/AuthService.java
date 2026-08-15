package cn.exitcode.richpeasants.admin.service;

import cn.exitcode.richpeasants.admin.dto.LoginRequest;
import cn.exitcode.richpeasants.admin.dto.LoginResponse;
import cn.exitcode.richpeasants.common.entity.AdminUser;
import cn.exitcode.richpeasants.common.enums.UserRole;
import cn.exitcode.richpeasants.common.exception.BusinessException;
import cn.exitcode.richpeasants.common.repository.AdminUserRepository;
import cn.exitcode.richpeasants.common.result.ResultCode;
import cn.exitcode.richpeasants.common.security.JwtProperties;
import cn.exitcode.richpeasants.common.security.JwtTokenProvider;
import cn.exitcode.richpeasants.common.security.LoginUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AuthService {

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;

    public AuthService(AdminUserRepository adminUserRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider,
                       JwtProperties jwtProperties) {
        this.adminUserRepository = adminUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.jwtProperties = jwtProperties;
    }

    public LoginResponse login(LoginRequest request) {
        AdminUser admin = adminUserRepository.findByUsername(request.getUsername().trim())
                .orElseThrow(() -> new BusinessException(ResultCode.UNAUTHORIZED, "用户名或密码错误"));
        if (!Boolean.TRUE.equals(admin.getEnabled())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "账号已禁用");
        }
        if (!passwordEncoder.matches(request.getPassword(), admin.getPassword())) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户名或密码错误");
        }
        return buildTokenResponse(admin);
    }

    public LoginResponse refresh(String refreshToken) {
        try {
            Claims claims = jwtTokenProvider.parseClaims(refreshToken);
            if (!jwtTokenProvider.isTokenType(claims, JwtTokenProvider.TYPE_REFRESH)) {
                throw new BusinessException(ResultCode.UNAUTHORIZED, "无效的刷新令牌");
            }
            String role = claims.get(JwtTokenProvider.CLAIM_ROLE, String.class);
            if (!UserRole.ADMIN.name().equals(role)) {
                throw new BusinessException(ResultCode.FORBIDDEN, "非站长令牌");
            }
            Long userId = claims.get(JwtTokenProvider.CLAIM_USER_ID, Long.class);
            AdminUser admin = adminUserRepository.findById(userId)
                    .orElseThrow(() -> new BusinessException(ResultCode.UNAUTHORIZED, "用户不存在"));
            if (!Boolean.TRUE.equals(admin.getEnabled())) {
                throw new BusinessException(ResultCode.FORBIDDEN, "账号不可用");
            }
            return buildTokenResponse(admin);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "刷新令牌无效或已过期");
        }
    }

    public LoginResponse.UserInfo currentUser(LoginUser loginUser) {
        if (loginUser.getRole() != UserRole.ADMIN) {
            throw new BusinessException(ResultCode.FORBIDDEN, "非站长账号");
        }
        AdminUser admin = adminUserRepository.findById(loginUser.getUserId())
                .orElseThrow(() -> new BusinessException(ResultCode.UNAUTHORIZED));
        return toUserInfo(admin);
    }

    private LoginResponse buildTokenResponse(AdminUser admin) {
        String accessToken = jwtTokenProvider.createAccessToken(admin.getId(), admin.getUsername(), UserRole.ADMIN);
        String refreshToken = jwtTokenProvider.createRefreshToken(admin.getId(), admin.getUsername(), UserRole.ADMIN);

        LoginResponse response = new LoginResponse();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setTokenType("Bearer");
        response.setExpiresIn(jwtProperties.getAccessTokenExpireSeconds());
        response.setUser(toUserInfo(admin));
        return response;
    }

    private LoginResponse.UserInfo toUserInfo(AdminUser admin) {
        LoginResponse.UserInfo info = new LoginResponse.UserInfo();
        info.setId(admin.getId());
        info.setUsername(admin.getUsername());
        info.setNickname(StringUtils.hasText(admin.getNickname()) ? admin.getNickname() : admin.getUsername());
        info.setRole(UserRole.ADMIN);
        return info;
    }
}
