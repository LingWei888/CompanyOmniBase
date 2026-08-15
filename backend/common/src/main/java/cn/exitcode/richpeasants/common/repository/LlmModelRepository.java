package cn.exitcode.richpeasants.common.repository;

import cn.exitcode.richpeasants.common.entity.LlmModel;
import cn.exitcode.richpeasants.common.enums.LlmModelPurpose;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LlmModelRepository extends JpaRepository<LlmModel, Long> {

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    Page<LlmModel> findAllByOrderByIdDesc(Pageable pageable);

    Page<LlmModel> findByPurposeOrderByIdDesc(LlmModelPurpose purpose, Pageable pageable);

    List<LlmModel> findByEnabledTrueOrderByIdAsc();

    List<LlmModel> findByPurposeAndEnabledTrueOrderByIdAsc(LlmModelPurpose purpose);

    Optional<LlmModel> findFirstByPurposeAndEnabledTrueOrderByIdAsc(LlmModelPurpose purpose);
}
