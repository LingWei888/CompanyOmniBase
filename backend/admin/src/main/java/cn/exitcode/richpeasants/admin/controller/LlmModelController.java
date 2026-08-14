package cn.exitcode.richpeasants.admin.controller;

import cn.exitcode.richpeasants.admin.dto.LlmModelRequest;
import cn.exitcode.richpeasants.admin.service.LlmModelService;
import cn.exitcode.richpeasants.common.entity.LlmModel;
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

@RestController
@RequestMapping("/api/admin/models")
public class LlmModelController {

    private final LlmModelService llmModelService;

    public LlmModelController(LlmModelService llmModelService) {
        this.llmModelService = llmModelService;
    }

    @GetMapping
    public ApiResult<PageResult<LlmModel>> page(@RequestParam(defaultValue = "1") int page,
                                                @RequestParam(defaultValue = "10") int size) {
        return ApiResult.ok(llmModelService.page(page, size));
    }

    @GetMapping("/{id}")
    public ApiResult<LlmModel> detail(@PathVariable Long id) {
        return ApiResult.ok(llmModelService.get(id));
    }

    @PostMapping
    public ApiResult<LlmModel> create(@Valid @RequestBody LlmModelRequest request) {
        return ApiResult.ok(llmModelService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResult<LlmModel> update(@PathVariable Long id, @Valid @RequestBody LlmModelRequest request) {
        return ApiResult.ok(llmModelService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        llmModelService.delete(id);
        return ApiResult.ok();
    }
}
