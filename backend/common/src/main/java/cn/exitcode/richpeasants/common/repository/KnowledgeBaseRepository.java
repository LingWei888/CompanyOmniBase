package cn.exitcode.richpeasants.common.repository;

import cn.exitcode.richpeasants.common.entity.KnowledgeBase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KnowledgeBaseRepository extends JpaRepository<KnowledgeBase, Long> {

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    Page<KnowledgeBase> findAllByOrderByIdDesc(Pageable pageable);

    List<KnowledgeBase> findAllByOrderByIdDesc();
}
