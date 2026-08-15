package cn.exitcode.richpeasants.common.security;

import cn.exitcode.richpeasants.common.entity.SysUser;
import cn.exitcode.richpeasants.common.enums.UserRole;
import cn.exitcode.richpeasants.common.repository.SysUserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * 仅加载前台用户表 sys_user。站长走 admin_user，不经过此处。
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final SysUserRepository sysUserRepository;

    public CustomUserDetailsService(SysUserRepository sysUserRepository) {
        this.sysUserRepository = sysUserRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = sysUserRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在: " + username));
        return new LoginUser(user.getId(), user.getUsername(), user.getPassword(), UserRole.USER, user.getEnabled());
    }
}
