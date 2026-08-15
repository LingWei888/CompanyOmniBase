package cn.exitcode.richpeasants.common.repository;

import cn.exitcode.richpeasants.common.entity.SysUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SysUserRepository extends JpaRepository<SysUser, Long> {

    Optional<SysUser> findByUsername(String username);

    boolean existsByUsername(String username);

    Page<SysUser> findAllByOrderByIdDesc(Pageable pageable);

    Page<SysUser> findByUsernameContainingIgnoreCaseOrderByIdDesc(String username, Pageable pageable);
}
