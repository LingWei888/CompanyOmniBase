package cn.exitcode.richpeasants.api.auth;

import cn.exitcode.richpeasants.common.entity.SysUser;
import cn.exitcode.richpeasants.common.enums.UserPlan;
import cn.exitcode.richpeasants.common.enums.UserRole;
import cn.exitcode.richpeasants.common.exception.BusinessException;
import cn.exitcode.richpeasants.common.repository.SysUserRepository;
import cn.exitcode.richpeasants.common.result.ResultCode;
import cn.exitcode.richpeasants.common.security.JwtProperties;
import cn.exitcode.richpeasants.common.security.JwtTokenProvider;
import cn.exitcode.richpeasants.common.security.LoginUser;
import cn.exitcode.richpeasants.common.storage.MinioStorageService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UserAuthService {

    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final SysUserRepository sysUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final MinioStorageService minioStorageService;

    public UserAuthService(JwtTokenProvider jwtTokenProvider,
                           JwtProperties jwtProperties,
                           SysUserRepository sysUserRepository,
                           PasswordEncoder passwordEncoder,
                           MinioStorageService minioStorageService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.jwtProperties = jwtProperties;
        this.sysUserRepository = sysUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.minioStorageService = minioStorageService;
    }

    @Transactional
    public UserAuthResponse register(RegisterRequest request) {
        String username = request.getUsername().trim();
        if (sysUserRepository.existsByUsername(username)) {
            throw new BusinessException(ResultCode.CONFLICT, "用户名已存在");
        }
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        String nickname = StringUtils.hasText(request.getNickname())
                ? request.getNickname().trim()
                : username;
        user.setNickname(nickname);
        user.setPlan(UserPlan.FREE);
        user.setEnabled(true);
        sysUserRepository.save(user);
        return buildTokenResponse(user);
    }

    public UserAuthResponse login(UserLoginRequest request) {
        SysUser user = sysUserRepository.findByUsername(request.getUsername().trim())
                .orElseThrow(() -> new BusinessException(ResultCode.UNAUTHORIZED, "用户名或密码错误"));
        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "账号已禁用");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户名或密码错误");
        }
        if (user.getPlan() == null) {
            user.setPlan(UserPlan.FREE);
            sysUserRepository.save(user);
        }
        return buildTokenResponse(user);
    }

    public UserAuthResponse refresh(String refreshToken) {
        try {
            Claims claims = jwtTokenProvider.parseClaims(refreshToken);
            if (!jwtTokenProvider.isTokenType(claims, JwtTokenProvider.TYPE_REFRESH)) {
                throw new BusinessException(ResultCode.UNAUTHORIZED, "无效的刷新令牌");
            }
            String role = claims.get(JwtTokenProvider.CLAIM_ROLE, String.class);
            if (!UserRole.USER.name().equals(role)) {
                throw new BusinessException(ResultCode.FORBIDDEN, "站长账号不能登录前台");
            }
            Long userId = claims.get(JwtTokenProvider.CLAIM_USER_ID, Long.class);
            SysUser user = sysUserRepository.findById(userId)
                    .orElseThrow(() -> new BusinessException(ResultCode.UNAUTHORIZED, "用户不存在"));
            if (!Boolean.TRUE.equals(user.getEnabled())) {
                throw new BusinessException(ResultCode.FORBIDDEN, "账号不可用");
            }
            return buildTokenResponse(user);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "刷新令牌无效或已过期");
        }
    }

    public UserAuthResponse.UserInfo currentUser(LoginUser loginUser) {
        requireAppUser(loginUser);
        SysUser user = sysUserRepository.findById(loginUser.getUserId())
                .orElseThrow(() -> new BusinessException(ResultCode.UNAUTHORIZED, "用户不存在"));
        return toUserInfo(user);
    }

    @Transactional
    public UserAuthResponse.UserInfo updateProfile(LoginUser loginUser, UpdateProfileRequest request) {
        requireAppUser(loginUser);
        SysUser user = sysUserRepository.findById(loginUser.getUserId())
                .orElseThrow(() -> new BusinessException(ResultCode.UNAUTHORIZED, "用户不存在"));
        if (request.getNickname() != null) {
            String nickname = request.getNickname().trim();
            user.setNickname(StringUtils.hasText(nickname) ? nickname : user.getUsername());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(StringUtils.hasText(request.getAvatarUrl()) ? request.getAvatarUrl().trim() : null);
        }
        sysUserRepository.save(user);
        return toUserInfo(user);
    }

    @Transactional
    public UserAuthResponse.UserInfo uploadAvatar(LoginUser loginUser, MultipartFile file) {
        requireAppUser(loginUser);
        SysUser user = sysUserRepository.findById(loginUser.getUserId())
                .orElseThrow(() -> new BusinessException(ResultCode.UNAUTHORIZED, "用户不存在"));
        String url = minioStorageService.uploadSiteAsset(file, "avatars");
        String oldAvatar = user.getAvatarUrl();
        user.setAvatarUrl(url);
        sysUserRepository.save(user);
        if (StringUtils.hasText(oldAvatar) && !oldAvatar.equals(url)) {
            try {
                minioStorageService.delete(oldAvatar);
            } catch (Exception ignored) {
                // 旧头像清理失败不阻断更新
            }
        }
        return toUserInfo(user);
    }

    @Transactional
    public void changePassword(LoginUser loginUser, ChangePasswordRequest request) {
        requireAppUser(loginUser);
        SysUser user = sysUserRepository.findById(loginUser.getUserId())
                .orElseThrow(() -> new BusinessException(ResultCode.UNAUTHORIZED, "用户不存在"));
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "原密码不正确");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        sysUserRepository.save(user);
    }

    private void requireAppUser(LoginUser loginUser) {
        if (loginUser == null || loginUser.getRole() != UserRole.USER) {
            throw new BusinessException(ResultCode.FORBIDDEN, "站长账号不能使用前台用户功能");
        }
    }

    private UserAuthResponse buildTokenResponse(SysUser user) {
        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getUsername(), UserRole.USER);
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId(), user.getUsername(), UserRole.USER);
        UserAuthResponse response = new UserAuthResponse();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setTokenType("Bearer");
        response.setExpiresIn(jwtProperties.getAccessTokenExpireSeconds());
        response.setUser(toUserInfo(user));
        return response;
    }

    private UserAuthResponse.UserInfo toUserInfo(SysUser user) {
        UserAuthResponse.UserInfo info = new UserAuthResponse.UserInfo();
        info.setId(user.getId());
        info.setUsername(user.getUsername());
        info.setNickname(StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername());
        info.setAvatarUrl(user.getAvatarUrl());
        info.setRole(UserRole.USER);
        info.setPlan(user.getPlan() == null ? UserPlan.FREE : user.getPlan());
        return info;
    }
}
