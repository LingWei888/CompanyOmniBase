package cn.exitcode.richpeasants.common.security;

import cn.exitcode.richpeasants.common.entity.SysUser;
import cn.exitcode.richpeasants.common.repository.SysUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final SysUserRepository sysUserRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = sysUserRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在: " + username));
        return new LoginUser(user.getId(), user.getUsername(), user.getPassword(), user.getRole(), user.getEnabled());
    }
}
