package cn.exitcode.richpeasants.admin.service;

import cn.exitcode.richpeasants.admin.dto.IngestOpsOverviewResponse;
import cn.exitcode.richpeasants.admin.dto.IngestQueueItemResponse;
import cn.exitcode.richpeasants.common.entity.KbDocument;
import cn.exitcode.richpeasants.common.enums.DocumentStatus;
import cn.exitcode.richpeasants.common.exception.BusinessException;
import cn.exitcode.richpeasants.common.repository.KbDocumentRepository;
import cn.exitcode.richpeasants.common.result.PageResult;
import cn.exitcode.richpeasants.common.result.ResultCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class IngestOpsService {

    private final KbDocumentRepository kbDocumentRepository;
    private final DocumentAdminService documentAdminService;

    public IngestOpsService(KbDocumentRepository kbDocumentRepository,
                            DocumentAdminService documentAdminService) {
        this.kbDocumentRepository = kbDocumentRepository;
        this.documentAdminService = documentAdminService;
    }

    public IngestOpsOverviewResponse overview() {
        IngestOpsOverviewResponse response = new IngestOpsOverviewResponse();
        Map<String, Long> counts = new HashMap<>();
        for (DocumentStatus status : DocumentStatus.values()) {
            long count = kbDocumentRepository.countByStatus(status);
            counts.put(status.name(), count);
        }
        response.setDocumentStatusCounts(counts);
        response.setWaitingEmbeddingCount(counts.getOrDefault(DocumentStatus.WAITING_EMBEDDING.name(), 0L));
        response.setEmbeddingCount(counts.getOrDefault(DocumentStatus.EMBEDDING.name(), 0L));
        response.setFailedCount(counts.getOrDefault(DocumentStatus.FAILED.name(), 0L));
        response.setReadyCount(counts.getOrDefault(DocumentStatus.READY.name(), 0L));
        response.setWaitingEmbedding(listQueue(DocumentStatus.WAITING_EMBEDDING, 1, 10).getRecords());
        response.setEmbedding(listQueue(DocumentStatus.EMBEDDING, 1, 10).getRecords());
        return response;
    }

    public PageResult<IngestQueueItemResponse> listQueue(DocumentStatus status, int page, int size) {
        if (status != DocumentStatus.WAITING_EMBEDDING && status != DocumentStatus.EMBEDDING) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "仅支持 WAITING_EMBEDDING / EMBEDDING 队列");
        }
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), normalizeSize(size));
        Page<KbDocument> data = kbDocumentRepository.findByStatusOrderByIdDesc(status, pageable);
        Page<IngestQueueItemResponse> mapped = data.map(this::toQueueItem);
        return PageResult.from(mapped);
    }

    public KbDocument startEmbedding(Long id) {
        return documentAdminService.startEmbedding(id);
    }

    public List<KbDocument> startEmbeddingBatch(List<Long> documentIds) {
        if (documentIds == null || documentIds.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "请至少选择一个文档");
        }
        return documentIds.stream()
                .distinct()
                .map(documentAdminService::startEmbedding)
                .collect(Collectors.toList());
    }

    private IngestQueueItemResponse toQueueItem(KbDocument document) {
        IngestQueueItemResponse item = new IngestQueueItemResponse();
        item.setId(document.getId());
        item.setKbId(document.getKbId());
        item.setTitle(document.getTitle());
        item.setStatus(document.getStatus());
        item.setChunkCount(document.getChunkCount());
        item.setParsedCharCount(document.getParsedCharCount());
        item.setErrorMessage(document.getErrorMessage());
        item.setUpdatedAt(document.getUpdatedAt());
        return item;
    }

    private int normalizeSize(int size) {
        if (size <= 0) {
            return 10;
        }
        return Math.min(size, 100);
    }
}
