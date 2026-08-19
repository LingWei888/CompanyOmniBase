package cn.exitcode.richpeasants.rag.controller;

import cn.exitcode.richpeasants.common.result.ApiResult;
import cn.exitcode.richpeasants.common.result.ResultCode;
import cn.exitcode.richpeasants.common.security.LoginUser;
import cn.exitcode.richpeasants.rag.dto.ChatSessionCreateRequest;
import cn.exitcode.richpeasants.rag.dto.ChatSessionDetailResponse;
import cn.exitcode.richpeasants.rag.dto.ChatSessionItemResponse;
import cn.exitcode.richpeasants.rag.dto.ChatSessionUpdateRequest;
import cn.exitcode.richpeasants.rag.service.ChatSessionService;
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
@RequestMapping("/api/auth/chat/sessions")
public class ChatSessionController {

    private final ChatSessionService chatSessionService;

    public ChatSessionController(ChatSessionService chatSessionService) {
        this.chatSessionService = chatSessionService;
    }

    @GetMapping
    public ApiResult<List<ChatSessionItemResponse>> list(@AuthenticationPrincipal LoginUser loginUser) {
        if (loginUser == null) {
            return ApiResult.fail(ResultCode.UNAUTHORIZED);
        }
        return ApiResult.ok(chatSessionService.list(loginUser));
    }

    @PostMapping
    public ApiResult<ChatSessionItemResponse> create(@AuthenticationPrincipal LoginUser loginUser,
                                                     @Valid @RequestBody(required = false) ChatSessionCreateRequest request) {
        if (loginUser == null) {
            return ApiResult.fail(ResultCode.UNAUTHORIZED);
        }
        ChatSessionCreateRequest body = request == null ? new ChatSessionCreateRequest() : request;
        return ApiResult.ok(chatSessionService.create(loginUser, body));
    }

    @GetMapping("/{id}")
    public ApiResult<ChatSessionDetailResponse> detail(@AuthenticationPrincipal LoginUser loginUser,
                                                     @PathVariable Long id) {
        if (loginUser == null) {
            return ApiResult.fail(ResultCode.UNAUTHORIZED);
        }
        return ApiResult.ok(chatSessionService.detail(loginUser, id));
    }

    @PutMapping("/{id}")
    public ApiResult<ChatSessionItemResponse> update(@AuthenticationPrincipal LoginUser loginUser,
                                                     @PathVariable Long id,
                                                     @Valid @RequestBody ChatSessionUpdateRequest request) {
        if (loginUser == null) {
            return ApiResult.fail(ResultCode.UNAUTHORIZED);
        }
        return ApiResult.ok(chatSessionService.update(loginUser, id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@AuthenticationPrincipal LoginUser loginUser, @PathVariable Long id) {
        if (loginUser == null) {
            return ApiResult.fail(ResultCode.UNAUTHORIZED);
        }
        chatSessionService.delete(loginUser, id);
        return ApiResult.ok();
    }
}
