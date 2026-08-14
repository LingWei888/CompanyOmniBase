package cn.exitcode.richpeasants.api.config;

import cn.exitcode.richpeasants.common.entity.SysUser;
import cn.exitcode.richpeasants.common.enums.UserRole;
import cn.exitcode.richpeasants.common.repository.SysUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminUserInitializer implements CommandLineRunner {

    private final SysUserRepository sysUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (!sysUserRepository.existsByUsername("admin")) {
            SysUser admin = new SysUser();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setNickname("站长");
            admin.setRole(UserRole.ADMIN);
            admin.setEnabled(true);
            sysUserRepository.save(admin);
            log.info("Initialized default admin user: admin / admin123");
        }

        if (!sysUserRepository.existsByUsername("user")) {
            SysUser user = new SysUser();
            user.setUsername("user");
            user.setPassword(passwordEncoder.encode("user123"));
            user.setNickname("普通用户");
            user.setRole(UserRole.USER);
            user.setEnabled(true);
            sysUserRepository.save(user);
            log.info("Initialized default user: user / user123");
        }
    }
}
