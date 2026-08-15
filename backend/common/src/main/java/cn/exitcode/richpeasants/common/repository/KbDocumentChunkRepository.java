package cn.exitcode.richpeasants.common.repository;

import cn.exitcode.richpeasants.common.entity.KbDocumentChunk;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface KbDocumentChunkRepository extends JpaRepository<KbDocumentChunk, Long> {

    List<KbDocumentChunk> findByDocumentIdOrderByChunkIndexAsc(Long documentId);

    Page<KbDocumentChunk> findByDocumentIdOrderByChunkIndexAsc(Long documentId, Pageable pageable);

    Optional<KbDocumentChunk> findByIdAndDocumentId(Long id, Long documentId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from KbDocumentChunk c where c.documentId = :documentId")
    void deleteByDocumentId(@Param("documentId") Long documentId);
}
