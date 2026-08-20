package cn.exitcode.richpeasants.rag.service;

import cn.exitcode.richpeasants.common.entity.LlmModel;
import cn.exitcode.richpeasants.common.enums.LlmModelPurpose;
import cn.exitcode.richpeasants.common.exception.BusinessException;
import cn.exitcode.richpeasants.common.repository.LlmModelRepository;
import cn.exitcode.richpeasants.common.result.ResultCode;
import cn.exitcode.richpeasants.rag.dto.TestdataGenRequest;
import cn.exitcode.richpeasants.rag.dto.TestdataGenResponse;
import cn.exitcode.richpeasants.rag.llm.ChatCompletionsClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@Service
public class TestdataGenService {

    private final LlmModelRepository llmModelRepository;
    private final ChatCompletionsClient chatCompletionsClient;
    private final ObjectMapper objectMapper;

    public TestdataGenService(LlmModelRepository llmModelRepository,
                              ChatCompletionsClient chatCompletionsClient,
                              ObjectMapper objectMapper) {
        this.llmModelRepository = llmModelRepository;
        this.chatCompletionsClient = chatCompletionsClient;
        this.objectMapper = objectMapper;
    }

    public TestdataGenResponse generate(TestdataGenRequest request) {
        LlmModel model = requireChatModel(request.getModelId());
        validateRequest(request);
        String python = runGenerate(model, request);
        return new TestdataGenResponse(python);
    }

    public void generateStream(TestdataGenRequest request, SseEmitter emitter) {
        try {
            LlmModel model = requireChatModel(request.getModelId());
            validateRequest(request);
            String userPrompt = TestdataGenPrompts.buildUserPrompt(request);

            StringBuilder full = new StringBuilder();
            chatCompletionsClient.stream(
                    model,
                    TestdataGenPrompts.SYSTEM_PROMPT,
                    userPrompt,
                    delta -> {
                        full.append(delta);
                        try {
                            sendEvent(emitter, "delta", java.util.Map.of("content", delta));
                        } catch (IOException ex) {
                            throw new BusinessException(ResultCode.INTERNAL_ERROR, "推送流式结果失败: " + ex.getMessage());
                        }
                    },
                    TestdataGenPrompts.GEN_TEMPERATURE);

            String python = TestdataGenPrompts.stripToPython(full.toString());
            if (!StringUtils.hasText(python)) {
                throw new BusinessException(ResultCode.INTERNAL_ERROR, "模型未返回有效的 Python 脚本");
            }
            sendEvent(emitter, "done", java.util.Map.of("python", python));
            emitter.complete();
        } catch (BusinessException ex) {
            try {
                sendEvent(emitter, "error", java.util.Map.of("message", ex.getMessage()));
            } catch (Exception ignored) {
                // ignore
            }
            emitter.completeWithError(ex);
        } catch (Exception ex) {
            try {
                sendEvent(emitter, "error", java.util.Map.of("message", "数据生成脚本生成失败: " + ex.getMessage()));
            } catch (Exception ignored) {
                // ignore
            }
            emitter.completeWithError(ex);
        }
    }

    private String runGenerate(LlmModel model, TestdataGenRequest request) {
        String userPrompt = TestdataGenPrompts.buildUserPrompt(request);
        String raw = chatCompletionsClient.complete(
                model,
                TestdataGenPrompts.SYSTEM_PROMPT,
                userPrompt,
                TestdataGenPrompts.GEN_TEMPERATURE);
        String python = TestdataGenPrompts.stripToPython(raw);
        if (!StringUtils.hasText(python)) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "模型未返回有效的 Python 脚本");
        }
        return python;
    }

    private void validateRequest(TestdataGenRequest request) {
        if (!StringUtils.hasText(request.getOriginalText())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "请填写原题全文");
        }
        SolutionCodeValidator.validateOptional(request.getSolutionCode());
        if (StringUtils.hasText(request.getSolutionCode())) {
            request.setSolutionCode(SolutionCodeValidator.normalize(request.getSolutionCode()));
        }
    }

    private LlmModel requireChatModel(Long modelId) {
        LlmModel chatModel = llmModelRepository.findById(modelId)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "对话模型不存在"));
        if (chatModel.getPurpose() != LlmModelPurpose.CHAT || !Boolean.TRUE.equals(chatModel.getEnabled())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "请选择已启用的对话模型");
        }
        if (!StringUtils.hasText(chatModel.getBaseUrl())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "对话模型未配置 baseUrl");
        }
        return chatModel;
    }

    private void sendEvent(SseEmitter emitter, String event, Object data) throws IOException {
        String json = objectMapper.writeValueAsString(data);
        emitter.send(SseEmitter.event().name(event).data(json, MediaType.TEXT_PLAIN));
    }
}
