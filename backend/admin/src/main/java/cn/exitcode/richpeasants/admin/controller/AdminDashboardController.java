package cn.exitcode.richpeasants.admin.controller;

import cn.exitcode.richpeasants.admin.service.IngestOpsService;
import cn.exitcode.richpeasants.common.result.ApiResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 后台受保护接口示例，用于 Day2 鉴权验收。
 */
@RestController
@RequestMapping("/api/admin")
public class AdminDashboardController {

    private final IngestOpsService ingestOpsService;

    public AdminDashboardController(IngestOpsService ingestOpsService) {
        this.ingestOpsService = ingestOpsService;
    }

    @GetMapping("/dashboard/overview")
    public ApiResult<Map<String, Object>> overview() {
        var ops = ingestOpsService.overview();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("title", "知识库管理后台");
        body.put("status", "ok");
        body.put("serverTime", LocalDateTime.now().toString());
        body.put("documentStatusCounts", ops.getDocumentStatusCounts());
        body.put("waitingEmbeddingCount", ops.getWaitingEmbeddingCount());
        body.put("embeddingCount", ops.getEmbeddingCount());
        body.put("failedCount", ops.getFailedCount());
        body.put("readyCount", ops.getReadyCount());
        return ApiResult.ok(body);
    }
}
