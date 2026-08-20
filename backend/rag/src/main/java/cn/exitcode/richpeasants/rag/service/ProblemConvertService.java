package cn.exitcode.richpeasants.rag.service;

import cn.exitcode.richpeasants.common.entity.LlmModel;
import cn.exitcode.richpeasants.common.enums.LlmModelPurpose;
import cn.exitcode.richpeasants.common.exception.BusinessException;
import cn.exitcode.richpeasants.common.repository.LlmModelRepository;
import cn.exitcode.richpeasants.common.result.ResultCode;
import cn.exitcode.richpeasants.rag.dto.ProblemConvertRequest;
import cn.exitcode.richpeasants.rag.dto.ProblemConvertResponse;
import cn.exitcode.richpeasants.rag.llm.ChatCompletionsClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@Service
public class ProblemConvertService {

    private final LlmModelRepository llmModelRepository;
    private final ChatCompletionsClient chatCompletionsClient;
    private final ObjectMapper objectMapper;

    public ProblemConvertService(LlmModelRepository llmModelRepository,
                                 ChatCompletionsClient chatCompletionsClient,
                                 ObjectMapper objectMapper) {
        this.llmModelRepository = llmModelRepository;
        this.chatCompletionsClient = chatCompletionsClient;
        this.objectMapper = objectMapper;
    }

    public ProblemConvertResponse convert(ProblemConvertRequest request) {
        LlmModel model = requireChatModel(request.getModelId());
        validateRequest(request);
        String markdown = runConvert(model, request);
        return new ProblemConvertResponse(markdown.trim());
    }

    public void convertStream(ProblemConvertRequest request, SseEmitter emitter) {
        try {
            LlmModel model = requireChatModel(request.getModelId());
            validateRequest(request);
            ProblemImmutableSections.Extracted extracted =
                    ProblemImmutableSections.extract(request.getOriginalText());
            String userPrompt = ProblemConvertPrompts.buildUserPrompt(request, extracted);

            StringBuilder full = new StringBuilder();
            chatCompletionsClient.stream(
                    model,
                    ProblemConvertPrompts.SYSTEM_PROMPT,
                    userPrompt,
                    delta -> {
                        full.append(delta);
                        try {
                            sendEvent(emitter, "delta", java.util.Map.of("content", delta));
                        } catch (IOException ex) {
                            throw new BusinessException(ResultCode.INTERNAL_ERROR, "推送流式结果失败: " + ex.getMessage());
                        }
                    },
                    ProblemConvertPrompts.CONVERT_TEMPERATURE);

            String markdown = postProcess(full.toString(), request, extracted);
            sendEvent(emitter, "done", java.util.Map.of("markdown", markdown));
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
                sendEvent(emitter, "error", java.util.Map.of("message", "题面转换失败: " + ex.getMessage()));
            } catch (Exception ignored) {
                // ignore
            }
            emitter.completeWithError(ex);
        }
    }

    private String runConvert(LlmModel model, ProblemConvertRequest request) {
        ProblemImmutableSections.Extracted extracted =
                ProblemImmutableSections.extract(request.getOriginalText());
        String userPrompt = ProblemConvertPrompts.buildUserPrompt(request, extracted);
        String raw = chatCompletionsClient.complete(
                model,
                ProblemConvertPrompts.SYSTEM_PROMPT,
                userPrompt,
                ProblemConvertPrompts.CONVERT_TEMPERATURE);
        return postProcess(raw, request, extracted);
    }

    private String postProcess(String raw,
                               ProblemConvertRequest request,
                               ProblemImmutableSections.Extracted extracted) {
        String targetTitle = request.getReferenceNickname().trim();
        String normalized = ProblemMarkdownNormalizer.normalize(raw, targetTitle);
        return ProblemImmutableSections.merge(normalized, extracted);
    }

    private void validateRequest(ProblemConvertRequest request) {
        if (!StringUtils.hasText(request.getReferenceNickname())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "请填写目标标题");
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
