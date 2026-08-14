package cn.exitcode.richpeasants.common.repository;

import cn.exitcode.richpeasants.common.entity.LlmModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LlmModelRepository extends JpaRepository<LlmModel, Long> {

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    Page<LlmModel> findAllByOrderByIdDesc(Pageable pageable);

    List<LlmModel> findByEnabledTrueOrderByIdAsc();
}
