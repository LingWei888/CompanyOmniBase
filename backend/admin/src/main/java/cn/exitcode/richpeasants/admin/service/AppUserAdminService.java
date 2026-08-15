package cn.exitcode.richpeasants.admin.service;

import cn.exitcode.richpeasants.admin.dto.AppUserCreateRequest;
import cn.exitcode.richpeasants.admin.dto.AppUserResetPasswordRequest;
import cn.exitcode.richpeasants.admin.dto.AppUserResponse;
import cn.exitcode.richpeasants.admin.dto.AppUserUpdateRequest;
import cn.exitcode.richpeasants.common.entity.SysUser;
import cn.exitcode.richpeasants.common.enums.UserPlan;
import cn.exitcode.richpeasants.common.exception.BusinessException;
import cn.exitcode.richpeasants.common.repository.SysUserRepository;
import cn.exitcode.richpeasants.common.result.PageResult;
import cn.exitcode.richpeasants.common.result.ResultCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AppUserAdminService {

    private final SysUserRepository sysUserRepository;
    private final PasswordEncoder passwordEncoder;

    public AppUserAdminService(SysUserRepository sysUserRepository, PasswordEncoder passwordEncoder) {
        this.sysUserRepository = sysUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public PageResult<AppUserResponse> page(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), normalizeSize(size));
        Page<SysUser> data;
        if (StringUtils.hasText(keyword)) {
            data = sysUserRepository.findByUsernameContainingIgnoreCaseOrderByIdDesc(keyword.trim(), pageable);
        } else {
            data = sysUserRepository.findAllByOrderByIdDesc(pageable);
        }
        return PageResult.from(data.map(this::toResponse));
    }

    public AppUserResponse get(Long id) {
        return toResponse(findUser(id));
    }

    @Transactional
    public AppUserResponse create(AppUserCreateRequest request) {
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
        user.setPlan(request.getPlan() == null ? UserPlan.FREE : request.getPlan());
        user.setEnabled(request.getEnabled() == null || request.getEnabled());
        return toResponse(sysUserRepository.save(user));
    }

    @Transactional
    public AppUserResponse update(Long id, AppUserUpdateRequest request) {
        SysUser user = findUser(id);
        if (request.getNickname() != null) {
            String nickname = request.getNickname().trim();
            user.setNickname(StringUtils.hasText(nickname) ? nickname : user.getUsername());
        }
        if (request.getPlan() != null) {
            user.setPlan(request.getPlan());
        }
        if (request.getEnabled() != null) {
            user.setEnabled(request.getEnabled());
        }
        return toResponse(sysUserRepository.save(user));
    }

    @Transactional
    public void resetPassword(Long id, AppUserResetPasswordRequest request) {
        SysUser user = findUser(id);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        sysUserRepository.save(user);
    }

    @Transactional
    public void delete(Long id) {
        SysUser user = findUser(id);
        sysUserRepository.delete(user);
    }

    private SysUser findUser(Long id) {
        return sysUserRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "用户不存在"));
    }

    private AppUserResponse toResponse(SysUser user) {
        AppUserResponse response = new AppUserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setNickname(StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername());
        response.setAvatarUrl(user.getAvatarUrl());
        response.setPlan(user.getPlan() == null ? UserPlan.FREE : user.getPlan());
        response.setEnabled(user.getEnabled());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());
        return response;
    }

    private int normalizeSize(int size) {
        if (size <= 0) {
            return 10;
        }
        return Math.min(size, 100);
    }
}
