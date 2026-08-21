package cn.exitcode.richpeasants.rag.controller;

import cn.exitcode.richpeasants.common.result.ApiResult;
import cn.exitcode.richpeasants.common.result.ResultCode;
import cn.exitcode.richpeasants.common.security.LoginUser;
import cn.exitcode.richpeasants.rag.dto.UserMemoryItemResponse;
import cn.exitcode.richpeasants.rag.service.UserMemoryService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/auth/memory")
public class UserMemoryController {

    private final UserMemoryService userMemoryService;

    public UserMemoryController(UserMemoryService userMemoryService) {
        this.userMemoryService = userMemoryService;
    }

    @GetMapping
    public ApiResult<List<UserMemoryItemResponse>> list(@AuthenticationPrincipal LoginUser loginUser) {
        if (loginUser == null) {
            return ApiResult.fail(ResultCode.UNAUTHORIZED);
        }
        return ApiResult.ok(userMemoryService.list(loginUser));
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@AuthenticationPrincipal LoginUser loginUser, @PathVariable Long id) {
        if (loginUser == null) {
            return ApiResult.fail(ResultCode.UNAUTHORIZED);
        }
        userMemoryService.delete(loginUser, id);
        return ApiResult.ok();
    }

    @DeleteMapping
    public ApiResult<Void> clearAll(@AuthenticationPrincipal LoginUser loginUser) {
        if (loginUser == null) {
            return ApiResult.fail(ResultCode.UNAUTHORIZED);
        }
        userMemoryService.clearAll(loginUser);
        return ApiResult.ok();
    }
}
