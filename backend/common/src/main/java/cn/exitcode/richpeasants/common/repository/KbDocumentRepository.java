package cn.exitcode.richpeasants.common.repository;

import cn.exitcode.richpeasants.common.entity.KbDocument;
import cn.exitcode.richpeasants.common.enums.DocumentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KbDocumentRepository extends JpaRepository<KbDocument, Long> {

    Page<KbDocument> findByKbIdAndStatusOrderByIdDesc(Long kbId, DocumentStatus status, Pageable pageable);

    Page<KbDocument> findByKbIdOrderByIdDesc(Long kbId, Pageable pageable);

    Page<KbDocument> findByStatusOrderByIdDesc(DocumentStatus status, Pageable pageable);

    Page<KbDocument> findAllByOrderByIdDesc(Pageable pageable);

    long countByKbId(Long kbId);

    List<KbDocument> findByStatusOrderByIdAsc(DocumentStatus status);
}
