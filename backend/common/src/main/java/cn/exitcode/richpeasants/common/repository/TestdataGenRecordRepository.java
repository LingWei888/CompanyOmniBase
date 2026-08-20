package cn.exitcode.richpeasants.common.repository;

import cn.exitcode.richpeasants.common.entity.TestdataGenRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TestdataGenRecordRepository extends JpaRepository<TestdataGenRecord, Long> {

    List<TestdataGenRecord> findByUserIdOrderByUpdatedAtDesc(Long userId);

    Optional<TestdataGenRecord> findByIdAndUserId(Long id, Long userId);
}
