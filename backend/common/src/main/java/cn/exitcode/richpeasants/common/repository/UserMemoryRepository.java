package cn.exitcode.richpeasants.common.repository;

import cn.exitcode.richpeasants.common.entity.UserMemory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserMemoryRepository extends JpaRepository<UserMemory, Long> {

    List<UserMemory> findByUserIdOrderByUpdatedAtDesc(Long userId);

    Optional<UserMemory> findByIdAndUserId(Long id, Long userId);

    List<UserMemory> findByUserIdAndIdIn(Long userId, Collection<Long> ids);

    long countByUserId(Long userId);
}
