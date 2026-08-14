package cn.exitcode.richpeasants.api.config;

import cn.exitcode.richpeasants.common.entity.SysUser;
import cn.exitcode.richpeasants.common.enums.UserPlan;
import cn.exitcode.richpeasants.common.enums.UserRole;
import cn.exitcode.richpeasants.common.repository.SysUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminUserInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminUserInitializer.class);

    private final SysUserRepository sysUserRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminUserInitializer(SysUserRepository sysUserRepository, PasswordEncoder passwordEncoder) {
        this.sysUserRepository = sysUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (!sysUserRepository.existsByUsername("admin")) {
            SysUser admin = new SysUser();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setNickname("站长");
            admin.setRole(UserRole.ADMIN);
            admin.setPlan(UserPlan.FREE);
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
            user.setPlan(UserPlan.FREE);
            user.setEnabled(true);
            sysUserRepository.save(user);
            log.info("Initialized default user: user / user123");
        }
    }
}
