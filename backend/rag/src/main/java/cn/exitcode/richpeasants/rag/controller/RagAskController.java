package cn.exitcode.richpeasants.rag.controller;

import cn.exitcode.richpeasants.common.result.ApiResult;
import cn.exitcode.richpeasants.rag.dto.RagAskRequest;
import cn.exitcode.richpeasants.rag.dto.RagAskResponse;
import cn.exitcode.richpeasants.rag.service.RagAskService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/public/chat")
public class RagAskController {

    private final RagAskService ragAskService;

    public RagAskController(RagAskService ragAskService) {
        this.ragAskService = ragAskService;
    }

    @PostMapping("/ask")
    public ApiResult<RagAskResponse> ask(@Valid @RequestBody RagAskRequest request) {
        return ApiResult.ok(ragAskService.ask(request));
    }

    @PostMapping(value = "/ask/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter askStream(@Valid @RequestBody RagAskRequest request, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("X-Accel-Buffering", "no");
        response.setHeader("Connection", "keep-alive");
        SseEmitter emitter = new SseEmitter(180_000L);
        emitter.onTimeout(emitter::complete);
        CompletableFuture.runAsync(() -> ragAskService.askStream(request, emitter));
        return emitter;
    }
}
