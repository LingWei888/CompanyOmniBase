package cn.exitcode.richpeasants.admin.controller;

import cn.exitcode.richpeasants.admin.dto.IngestOpsOverviewResponse;
import cn.exitcode.richpeasants.admin.dto.IngestQueueItemResponse;
import cn.exitcode.richpeasants.admin.dto.StartEmbeddingBatchRequest;
import cn.exitcode.richpeasants.admin.service.IngestOpsService;
import cn.exitcode.richpeasants.common.entity.KbDocument;
import cn.exitcode.richpeasants.common.enums.DocumentStatus;
import cn.exitcode.richpeasants.common.result.ApiResult;
import cn.exitcode.richpeasants.common.result.PageResult;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/ingest")
public class IngestOpsController {

    private final IngestOpsService ingestOpsService;

    public IngestOpsController(IngestOpsService ingestOpsService) {
        this.ingestOpsService = ingestOpsService;
    }

    @GetMapping("/ops/overview")
    public ApiResult<IngestOpsOverviewResponse> overview() {
        return ApiResult.ok(ingestOpsService.overview());
    }

    @GetMapping("/embedding/waiting")
    public ApiResult<PageResult<IngestQueueItemResponse>> waitingQueue(@RequestParam(defaultValue = "1") int page,
                                                                       @RequestParam(defaultValue = "20") int size) {
        return ApiResult.ok(ingestOpsService.listQueue(DocumentStatus.WAITING_EMBEDDING, page, size));
    }

    @GetMapping("/embedding/running")
    public ApiResult<PageResult<IngestQueueItemResponse>> runningQueue(@RequestParam(defaultValue = "1") int page,
                                                                       @RequestParam(defaultValue = "20") int size) {
        return ApiResult.ok(ingestOpsService.listQueue(DocumentStatus.EMBEDDING, page, size));
    }

    @PostMapping("/embedding/start/{id}")
    public ApiResult<KbDocument> startEmbedding(@PathVariable Long id) {
        return ApiResult.ok(ingestOpsService.startEmbedding(id));
    }

    @PostMapping("/embedding/start-batch")
    public ApiResult<List<KbDocument>> startEmbeddingBatch(@Valid @RequestBody StartEmbeddingBatchRequest request) {
        return ApiResult.ok(ingestOpsService.startEmbeddingBatch(request.getDocumentIds()));
    }
}
