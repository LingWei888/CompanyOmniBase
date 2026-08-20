package cn.exitcode.richpeasants.rag.controller;

import cn.exitcode.richpeasants.common.result.ApiResult;
import cn.exitcode.richpeasants.common.result.ResultCode;
import cn.exitcode.richpeasants.common.security.LoginUser;
import cn.exitcode.richpeasants.rag.dto.ProblemConvertRecordDetailResponse;
import cn.exitcode.richpeasants.rag.dto.ProblemConvertRecordItemResponse;
import cn.exitcode.richpeasants.rag.dto.ProblemConvertRecordUpsertRequest;
import cn.exitcode.richpeasants.rag.service.ProblemConvertRecordService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/auth/agents/problem-convert/records")
public class ProblemConvertRecordController {

    private final ProblemConvertRecordService problemConvertRecordService;

    public ProblemConvertRecordController(ProblemConvertRecordService problemConvertRecordService) {
        this.problemConvertRecordService = problemConvertRecordService;
    }

    @GetMapping
    public ApiResult<List<ProblemConvertRecordItemResponse>> list(@AuthenticationPrincipal LoginUser loginUser) {
        if (loginUser == null) {
            return ApiResult.fail(ResultCode.UNAUTHORIZED);
        }
        return ApiResult.ok(problemConvertRecordService.list(loginUser));
    }

    @PostMapping
    public ApiResult<ProblemConvertRecordDetailResponse> create(@AuthenticationPrincipal LoginUser loginUser) {
        if (loginUser == null) {
            return ApiResult.fail(ResultCode.UNAUTHORIZED);
        }
        return ApiResult.ok(problemConvertRecordService.create(loginUser));
    }

    @GetMapping("/{id}")
    public ApiResult<ProblemConvertRecordDetailResponse> detail(@AuthenticationPrincipal LoginUser loginUser,
                                                                @PathVariable Long id) {
        if (loginUser == null) {
            return ApiResult.fail(ResultCode.UNAUTHORIZED);
        }
        return ApiResult.ok(problemConvertRecordService.detail(loginUser, id));
    }

    @PutMapping("/{id}")
    public ApiResult<ProblemConvertRecordDetailResponse> update(@AuthenticationPrincipal LoginUser loginUser,
                                                                @PathVariable Long id,
                                                                @Valid @RequestBody ProblemConvertRecordUpsertRequest request) {
        if (loginUser == null) {
            return ApiResult.fail(ResultCode.UNAUTHORIZED);
        }
        return ApiResult.ok(problemConvertRecordService.update(loginUser, id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@AuthenticationPrincipal LoginUser loginUser, @PathVariable Long id) {
        if (loginUser == null) {
            return ApiResult.fail(ResultCode.UNAUTHORIZED);
        }
        problemConvertRecordService.delete(loginUser, id);
        return ApiResult.ok();
    }
}
