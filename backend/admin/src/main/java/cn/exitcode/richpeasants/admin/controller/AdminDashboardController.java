package cn.exitcode.richpeasants.admin.controller;

import cn.exitcode.richpeasants.common.result.ApiResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 后台受保护接口示例，用于 Day2 鉴权验收。
 */
@RestController
@RequestMapping("/api/admin")
public class AdminDashboardController {

    @GetMapping("/dashboard/overview")
    public ApiResult<Map<String, Object>> overview() {
        return ApiResult.ok(Map.of(
                "title", "知识库管理后台",
                "status", "ok",
                "serverTime", LocalDateTime.now().toString()
        ));
    }
}
