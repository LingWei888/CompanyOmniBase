package cn.exitcode.richpeasants.admin.service;

import cn.exitcode.richpeasants.admin.dto.LoginRequest;
import cn.exitcode.richpeasants.admin.dto.LoginResponse;
import cn.exitcode.richpeasants.common.entity.SysUser;
import cn.exitcode.richpeasants.common.enums.UserRole;
import cn.exitcode.richpeasants.common.exception.BusinessException;
import cn.exitcode.richpeasants.common.repository.SysUserRepository;
import cn.exitcode.richpeasants.common.result.ResultCode;
import cn.exitcode.richpeasants.common.security.JwtProperties;
import cn.exitcode.richpeasants.common.security.JwtTokenProvider;
import cn.exitcode.richpeasants.common.security.LoginUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final SysUserRepository sysUserRepository;

    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        if (loginUser.getRole() != UserRole.ADMIN) {
            throw new BusinessException(ResultCode.FORBIDDEN, "非管理员账号无法登录后台");
        }
        return buildTokenResponse(loginUser.getUserId(), loginUser.getUsername(), loginUser.getRole());
    }

    public LoginResponse refresh(String refreshToken) {
        try {
            Claims claims = jwtTokenProvider.parseClaims(refreshToken);
            if (!jwtTokenProvider.isTokenType(claims, JwtTokenProvider.TYPE_REFRESH)) {
                throw new BusinessException(ResultCode.UNAUTHORIZED, "无效的刷新令牌");
            }
            Long userId = claims.get(JwtTokenProvider.CLAIM_USER_ID, Long.class);
            SysUser user = sysUserRepository.findById(userId)
                    .orElseThrow(() -> new BusinessException(ResultCode.UNAUTHORIZED, "用户不存在"));
            if (!Boolean.TRUE.equals(user.getEnabled()) || user.getRole() != UserRole.ADMIN) {
                throw new BusinessException(ResultCode.FORBIDDEN, "账号不可用");
            }
            return buildTokenResponse(user.getId(), user.getUsername(), user.getRole());
        } catch (JwtException | IllegalArgumentException ex) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "刷新令牌无效或已过期");
        }
    }

    public LoginResponse.UserInfo currentUser(LoginUser loginUser) {
        SysUser user = sysUserRepository.findById(loginUser.getUserId())
                .orElseThrow(() -> new BusinessException(ResultCode.UNAUTHORIZED));
        return LoginResponse.UserInfo.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .role(user.getRole())
                .build();
    }

    private LoginResponse buildTokenResponse(Long userId, String username, UserRole role) {
        String accessToken = jwtTokenProvider.createAccessToken(userId, username, role);
        String refreshToken = jwtTokenProvider.createRefreshToken(userId, username, role);
        SysUser user = sysUserRepository.findById(userId).orElse(null);
        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtProperties.getAccessTokenExpireSeconds())
                .user(LoginResponse.UserInfo.builder()
                        .id(userId)
                        .username(username)
                        .nickname(user != null ? user.getNickname() : username)
                        .role(role)
                        .build())
                .build();
    }
}
