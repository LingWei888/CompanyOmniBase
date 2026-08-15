package cn.exitcode.richpeasants.admin.controller;

import cn.exitcode.richpeasants.admin.dto.AppUserCreateRequest;
import cn.exitcode.richpeasants.admin.dto.AppUserResetPasswordRequest;
import cn.exitcode.richpeasants.admin.dto.AppUserResponse;
import cn.exitcode.richpeasants.admin.dto.AppUserUpdateRequest;
import cn.exitcode.richpeasants.admin.service.AppUserAdminService;
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
@RequestMapping("/api/admin/users")
public class AppUserAdminController {

    private final AppUserAdminService appUserAdminService;

    public AppUserAdminController(AppUserAdminService appUserAdminService) {
        this.appUserAdminService = appUserAdminService;
    }

    @GetMapping
    public ApiResult<PageResult<AppUserResponse>> page(@RequestParam(required = false) String keyword,
                                                       @RequestParam(defaultValue = "1") int page,
                                                       @RequestParam(defaultValue = "10") int size) {
        return ApiResult.ok(appUserAdminService.page(keyword, page, size));
    }

    @GetMapping("/{id}")
    public ApiResult<AppUserResponse> detail(@PathVariable Long id) {
        return ApiResult.ok(appUserAdminService.get(id));
    }

    @PostMapping
    public ApiResult<AppUserResponse> create(@Valid @RequestBody AppUserCreateRequest request) {
        return ApiResult.ok(appUserAdminService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResult<AppUserResponse> update(@PathVariable Long id,
                                             @Valid @RequestBody AppUserUpdateRequest request) {
        return ApiResult.ok(appUserAdminService.update(id, request));
    }

    @PutMapping("/{id}/password")
    public ApiResult<Void> resetPassword(@PathVariable Long id,
                                         @Valid @RequestBody AppUserResetPasswordRequest request) {
        appUserAdminService.resetPassword(id, request);
        return ApiResult.ok();
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        appUserAdminService.delete(id);
        return ApiResult.ok();
    }
}
