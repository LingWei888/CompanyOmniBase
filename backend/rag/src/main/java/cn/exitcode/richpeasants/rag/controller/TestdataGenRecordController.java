package cn.exitcode.richpeasants.rag.controller;

import cn.exitcode.richpeasants.common.result.ApiResult;
import cn.exitcode.richpeasants.common.result.ResultCode;
import cn.exitcode.richpeasants.common.security.LoginUser;
import cn.exitcode.richpeasants.rag.dto.TestdataGenRecordDetailResponse;
import cn.exitcode.richpeasants.rag.dto.TestdataGenRecordItemResponse;
import cn.exitcode.richpeasants.rag.dto.TestdataGenRecordUpsertRequest;
import cn.exitcode.richpeasants.rag.service.TestdataGenRecordService;
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
@RequestMapping("/api/auth/agents/testdata-gen/records")
public class TestdataGenRecordController {

    private final TestdataGenRecordService testdataGenRecordService;

    public TestdataGenRecordController(TestdataGenRecordService testdataGenRecordService) {
        this.testdataGenRecordService = testdataGenRecordService;
    }

    @GetMapping
    public ApiResult<List<TestdataGenRecordItemResponse>> list(@AuthenticationPrincipal LoginUser loginUser) {
        if (loginUser == null) {
            return ApiResult.fail(ResultCode.UNAUTHORIZED);
        }
        return ApiResult.ok(testdataGenRecordService.list(loginUser));
    }

    @PostMapping
    public ApiResult<TestdataGenRecordDetailResponse> create(@AuthenticationPrincipal LoginUser loginUser) {
        if (loginUser == null) {
            return ApiResult.fail(ResultCode.UNAUTHORIZED);
        }
        return ApiResult.ok(testdataGenRecordService.create(loginUser));
    }

    @GetMapping("/{id}")
    public ApiResult<TestdataGenRecordDetailResponse> detail(@AuthenticationPrincipal LoginUser loginUser,
                                                             @PathVariable Long id) {
        if (loginUser == null) {
            return ApiResult.fail(ResultCode.UNAUTHORIZED);
        }
        return ApiResult.ok(testdataGenRecordService.detail(loginUser, id));
    }

    @PutMapping("/{id}")
    public ApiResult<TestdataGenRecordDetailResponse> update(@AuthenticationPrincipal LoginUser loginUser,
                                                             @PathVariable Long id,
                                                             @Valid @RequestBody TestdataGenRecordUpsertRequest request) {
        if (loginUser == null) {
            return ApiResult.fail(ResultCode.UNAUTHORIZED);
        }
        return ApiResult.ok(testdataGenRecordService.update(loginUser, id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@AuthenticationPrincipal LoginUser loginUser, @PathVariable Long id) {
        if (loginUser == null) {
            return ApiResult.fail(ResultCode.UNAUTHORIZED);
        }
        testdataGenRecordService.delete(loginUser, id);
        return ApiResult.ok();
    }
}
