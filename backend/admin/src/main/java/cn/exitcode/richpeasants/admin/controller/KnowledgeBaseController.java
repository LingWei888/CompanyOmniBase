package cn.exitcode.richpeasants.admin.controller;

import cn.exitcode.richpeasants.admin.dto.KnowledgeBaseRequest;
import cn.exitcode.richpeasants.admin.service.KnowledgeBaseService;
import cn.exitcode.richpeasants.common.entity.KnowledgeBase;
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

import java.util.List;

@RestController
@RequestMapping("/api/admin/knowledge-bases")
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;

    public KnowledgeBaseController(KnowledgeBaseService knowledgeBaseService) {
        this.knowledgeBaseService = knowledgeBaseService;
    }

    @GetMapping
    public ApiResult<PageResult<KnowledgeBase>> page(@RequestParam(defaultValue = "1") int page,
                                                     @RequestParam(defaultValue = "10") int size) {
        return ApiResult.ok(knowledgeBaseService.page(page, size));
    }

    @GetMapping("/options")
    public ApiResult<List<KnowledgeBase>> options() {
        return ApiResult.ok(knowledgeBaseService.options());
    }

    @GetMapping("/{id}")
    public ApiResult<KnowledgeBase> detail(@PathVariable Long id) {
        return ApiResult.ok(knowledgeBaseService.get(id));
    }

    @PostMapping
    public ApiResult<KnowledgeBase> create(@Valid @RequestBody KnowledgeBaseRequest request) {
        return ApiResult.ok(knowledgeBaseService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResult<KnowledgeBase> update(@PathVariable Long id, @Valid @RequestBody KnowledgeBaseRequest request) {
        return ApiResult.ok(knowledgeBaseService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        knowledgeBaseService.delete(id);
        return ApiResult.ok();
    }
}
