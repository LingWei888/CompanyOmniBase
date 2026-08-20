package cn.exitcode.richpeasants.common.repository;

import cn.exitcode.richpeasants.common.entity.ProblemConvertRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProblemConvertRecordRepository extends JpaRepository<ProblemConvertRecord, Long> {

    List<ProblemConvertRecord> findByUserIdOrderByUpdatedAtDesc(Long userId);

    Optional<ProblemConvertRecord> findByIdAndUserId(Long id, Long userId);
}
