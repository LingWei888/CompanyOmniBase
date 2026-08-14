package cn.exitcode.richpeasants.admin.controller;

import cn.exitcode.richpeasants.admin.dto.DocumentUpdateRequest;
import cn.exitcode.richpeasants.admin.service.DocumentAdminService;
import cn.exitcode.richpeasants.common.entity.KbDocument;
import cn.exitcode.richpeasants.common.enums.DocumentStatus;
import cn.exitcode.richpeasants.common.result.ApiResult;
import cn.exitcode.richpeasants.common.result.PageResult;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/documents")
public class DocumentAdminController {

    private final DocumentAdminService documentAdminService;

    public DocumentAdminController(DocumentAdminService documentAdminService) {
        this.documentAdminService = documentAdminService;
    }

    @GetMapping
    public ApiResult<PageResult<KbDocument>> page(@RequestParam(required = false) Long kbId,
                                                  @RequestParam(required = false) DocumentStatus status,
                                                  @RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "10") int size) {
        return ApiResult.ok(documentAdminService.page(kbId, status, page, size));
    }

    @GetMapping("/{id}")
    public ApiResult<KbDocument> detail(@PathVariable Long id) {
        return ApiResult.ok(documentAdminService.get(id));
    }

    @PostMapping("/upload")
    public ApiResult<KbDocument> upload(@RequestParam Long kbId,
                                        @RequestParam(required = false) String title,
                                        @RequestParam("file") MultipartFile file) {
        return ApiResult.ok(documentAdminService.upload(kbId, title, file));
    }

    @PutMapping("/{id}")
    public ApiResult<KbDocument> update(@PathVariable Long id, @Valid @RequestBody DocumentUpdateRequest request) {
        return ApiResult.ok(documentAdminService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        documentAdminService.delete(id);
        return ApiResult.ok();
    }
}
