package cn.exitcode.richpeasants.rag.controller;

import cn.exitcode.richpeasants.common.exception.BusinessException;
import cn.exitcode.richpeasants.common.result.ApiResult;
import cn.exitcode.richpeasants.common.result.ResultCode;
import cn.exitcode.richpeasants.common.security.LoginUser;
import cn.exitcode.richpeasants.rag.dto.ProblemConvertRequest;
import cn.exitcode.richpeasants.rag.dto.ProblemConvertResponse;
import cn.exitcode.richpeasants.rag.service.ProblemConvertService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/auth/agents/problem-convert")
public class ProblemConvertController {

    private final ProblemConvertService problemConvertService;

    public ProblemConvertController(ProblemConvertService problemConvertService) {
        this.problemConvertService = problemConvertService;
    }

    @PostMapping
    public ApiResult<ProblemConvertResponse> convert(@AuthenticationPrincipal LoginUser loginUser,
                                                     @Valid @RequestBody ProblemConvertRequest request) {
        requireLogin(loginUser);
        return ApiResult.ok(problemConvertService.convert(request));
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter convertStream(@AuthenticationPrincipal LoginUser loginUser,
                                    @Valid @RequestBody ProblemConvertRequest request,
                                    HttpServletResponse response) {
        requireLogin(loginUser);
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("X-Accel-Buffering", "no");
        response.setHeader("Connection", "keep-alive");
        SseEmitter emitter = new SseEmitter(180_000L);
        emitter.onTimeout(emitter::complete);
        CompletableFuture.runAsync(() -> problemConvertService.convertStream(request, emitter));
        return emitter;
    }

    private static void requireLogin(LoginUser loginUser) {
        if (loginUser == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录后再使用题意修改智能体");
        }
    }
}
