package cn.exitcode.richpeasants.admin.controller;

import cn.exitcode.richpeasants.admin.dto.SysConfigItemRequest;
import cn.exitcode.richpeasants.admin.service.SysConfigService;
import cn.exitcode.richpeasants.common.entity.SysConfig;
import cn.exitcode.richpeasants.common.result.ApiResult;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/system/config")
public class SysConfigController {

    private final SysConfigService sysConfigService;

    public SysConfigController(SysConfigService sysConfigService) {
        this.sysConfigService = sysConfigService;
    }

    @GetMapping
    public ApiResult<List<SysConfig>> list() {
        return ApiResult.ok(sysConfigService.list());
    }

    @GetMapping("/map")
    public ApiResult<Map<String, String>> map() {
        return ApiResult.ok(sysConfigService.asMap());
    }

    @PutMapping
    public ApiResult<List<SysConfig>> save(@Valid @RequestBody List<SysConfigItemRequest> items) {
        return ApiResult.ok(sysConfigService.saveBatch(items));
    }

    @PostMapping("/logo")
    public ApiResult<Map<String, String>> uploadLogo(@RequestParam("file") MultipartFile file) {
        String url = sysConfigService.uploadLogo(file);
        return ApiResult.ok(Map.of("url", url));
    }
}
