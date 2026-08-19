package cn.exitcode.richpeasants.rag.service;

import cn.exitcode.richpeasants.common.entity.ChatSession;
import cn.exitcode.richpeasants.common.entity.KbDocument;
import cn.exitcode.richpeasants.common.entity.KnowledgeBase;
import cn.exitcode.richpeasants.common.entity.LlmModel;
import cn.exitcode.richpeasants.common.enums.LlmModelPurpose;
import cn.exitcode.richpeasants.common.exception.BusinessException;
import cn.exitcode.richpeasants.common.repository.KbDocumentRepository;
import cn.exitcode.richpeasants.common.repository.KnowledgeBaseRepository;
import cn.exitcode.richpeasants.common.repository.LlmModelRepository;
import cn.exitcode.richpeasants.common.result.ResultCode;
import cn.exitcode.richpeasants.common.security.LoginUser;
import cn.exitcode.richpeasants.ingest.embedding.EmbeddingClient;
import cn.exitcode.richpeasants.ingest.es.ChunkVectorStore;
import cn.exitcode.richpeasants.rag.agent.AgentOrchestrator;
import cn.exitcode.richpeasants.rag.config.RagAppProperties;
import cn.exitcode.richpeasants.rag.dto.RagAskRequest;
import cn.exitcode.richpeasants.rag.dto.RagAskResponse;
import cn.exitcode.richpeasants.rag.dto.RagCitation;
import cn.exitcode.richpeasants.rag.llm.ChatCompletionsClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RagAskService {

    private static final Logger log = LoggerFactory.getLogger(RagAskService.class);

    private static final String RAG_SYSTEM_PROMPT = """
            你是企业知识库智能助手。请只根据用户提供的「参考资料」回答问题。
            若资料不足以支撑结论，请明确说明无法从知识库得出答案，不要编造。
            回答使用简洁中文；涉及具体事实时可用 [1][2] 标注对应资料编号。
            输出代码时必须使用标准 Markdown 围栏：``` 单独成行，语言标记与代码之间换行，代码块结束后再用 ``` 单独成行关闭；不要把标题和 ``` 粘在同一行。
            """;

    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final LlmModelRepository llmModelRepository;
    private final KbDocumentRepository kbDocumentRepository;
    private final EmbeddingClient embeddingClient;
    private final ChunkVectorStore chunkVectorStore;
    private final ChatCompletionsClient chatCompletionsClient;
    private final AgentOrchestrator agentOrchestrator;
    private final RagAppProperties ragAppProperties;
    private final ObjectMapper objectMapper;
    private final ChatSessionService chatSessionService;

    public RagAskService(KnowledgeBaseRepository knowledgeBaseRepository,
                         LlmModelRepository llmModelRepository,
                         KbDocumentRepository kbDocumentRepository,
                         EmbeddingClient embeddingClient,
                         ChunkVectorStore chunkVectorStore,
                         ChatCompletionsClient chatCompletionsClient,
                         AgentOrchestrator agentOrchestrator,
                         RagAppProperties ragAppProperties,
                         ObjectMapper objectMapper,
                         ChatSessionService chatSessionService) {
        this.knowledgeBaseRepository = knowledgeBaseRepository;
        this.llmModelRepository = llmModelRepository;
        this.kbDocumentRepository = kbDocumentRepository;
        this.embeddingClient = embeddingClient;
        this.chunkVectorStore = chunkVectorStore;
        this.chatCompletionsClient = chatCompletionsClient;
        this.agentOrchestrator = agentOrchestrator;
        this.ragAppProperties = ragAppProperties;
        this.objectMapper = objectMapper;
        this.chatSessionService = chatSessionService;
    }

    public RagAskResponse ask(RagAskRequest request, LoginUser loginUser) {
        PreparedAsk prepared = prepare(request, loginUser);
        persistUserMessage(prepared);
        if (prepared.ragEnabled() && prepared.hits().isEmpty()) {
            String empty = "未在所选知识库中检索到相关内容。请确认文档已入库完成（状态为 READY），且使用了当前启用的 Embedding 模型。";
            persistAssistantMessage(prepared, empty, List.of());
            return new RagAskResponse(
                    empty,
                    prepared.kbIds(),
                    prepared.kbNames(),
                    prepared.chatModel().getId(),
                    prepared.chatModel().getName(),
                    List.of()
            );
        }
        String answer;
        if (!prepared.ragEnabled()) {
            answer = agentOrchestrator.run(
                    prepared.chatModel(),
                    prepared.userQuestion(),
                    (name, display) -> { },
                    delta -> { }
            );
        } else {
            answer = chatCompletionsClient.complete(
                    prepared.chatModel(), prepared.systemPrompt(), prepared.userPrompt());
        }
        persistAssistantMessage(prepared, answer, prepared.citations());
        return new RagAskResponse(
                answer,
                prepared.kbIds(),
                prepared.kbNames(),
                prepared.chatModel().getId(),
                prepared.chatModel().getName(),
                prepared.citations()
        );
    }

    public void askStream(RagAskRequest request, LoginUser loginUser, SseEmitter emitter) {
        try {
            PreparedAsk prepared = prepare(request, loginUser);
            persistUserMessage(prepared);
            sendEvent(emitter, "meta", Map.of(
                    "kbIds", prepared.kbIds(),
                    "kbNames", prepared.kbNames(),
                    "modelId", prepared.chatModel().getId(),
                    "modelName", prepared.chatModel().getName(),
                    "ragEnabled", prepared.ragEnabled(),
                    "agentEnabled", !prepared.ragEnabled()
            ));

            if (prepared.ragEnabled() && prepared.hits().isEmpty()) {
                String empty = "未在所选知识库中检索到相关内容。请确认文档已入库完成（状态为 READY），且使用了当前启用的 Embedding 模型。";
                sendEvent(emitter, "citations", List.of());
                sendEvent(emitter, "delta", Map.of("content", empty));
                sendEvent(emitter, "done", Map.of("answer", empty));
                persistAssistantMessage(prepared, empty, List.of());
                emitter.complete();
                return;
            }

            sendEvent(emitter, "citations", prepared.citations());

            StringBuilder full = new StringBuilder();
            if (!prepared.ragEnabled()) {
                agentOrchestrator.run(
                        prepared.chatModel(),
                        prepared.userQuestion(),
                        (toolName, display) -> {
                            try {
                                sendEvent(emitter, "tool", Map.of(
                                        "name", toolName,
                                        "message", display
                                ));
                            } catch (IOException ex) {
                                throw new BusinessException(ResultCode.INTERNAL_ERROR,
                                        "SSE 推送失败: " + ex.getMessage());
                            }
                        },
                        delta -> {
                            full.append(delta);
                            try {
                                sendEvent(emitter, "delta", Map.of("content", delta));
                            } catch (IOException ex) {
                                throw new BusinessException(ResultCode.INTERNAL_ERROR,
                                        "SSE 推送失败: " + ex.getMessage());
                            }
                        });
            } else {
                chatCompletionsClient.stream(
                        prepared.chatModel(),
                        prepared.systemPrompt(),
                        prepared.userPrompt(),
                        delta -> {
                            full.append(delta);
                            try {
                                sendEvent(emitter, "delta", Map.of("content", delta));
                            } catch (IOException ex) {
                                throw new BusinessException(ResultCode.INTERNAL_ERROR, "SSE 推送失败: " + ex.getMessage());
                            }
                        });
            }

            sendEvent(emitter, "done", Map.of("answer", full.toString()));
            persistAssistantMessage(prepared, full.toString(), prepared.citations());
            emitter.complete();
        } catch (BusinessException ex) {
            try {
                sendEvent(emitter, "error", Map.of("message", ex.getMessage() == null ? "问答失败" : ex.getMessage()));
            } catch (Exception ignored) {
                // ignore
            }
            emitter.completeWithError(ex);
        } catch (Exception ex) {
            log.error("askStream failed", ex);
            try {
                sendEvent(emitter, "error", Map.of("message", "问答失败: " + ex.getMessage()));
            } catch (Exception ignored) {
                // ignore
            }
            emitter.completeWithError(ex);
        }
    }

    private PreparedAsk prepare(RagAskRequest request, LoginUser loginUser) {
        String question = request.getQuestion() == null ? "" : request.getQuestion().trim();
        if (!StringUtils.hasText(question)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "问题不能为空");
        }

        ChatSession session = resolveSession(request, loginUser);
        ResolvedKbs resolved = resolveKnowledgeBases(request);
        LlmModel chatModel = requireChatModel(request.getModelId());

        // 未选知识库：走 Agent（工具：时间 / 天气 / Tavily 搜索）
        if (!resolved.ragEnabled()) {
            log.debug("Agent mode: no knowledge base selected");
            return new PreparedAsk(
                    List.of(),
                    List.of(),
                    chatModel,
                    List.of(),
                    List.of(),
                    question,
                    AgentOrchestrator.AGENT_SYSTEM_PROMPT,
                    false,
                    session,
                    question
            );
        }

        LlmModel embeddingModel = embeddingClient.requireEmbeddingModel();
        int topK = request.getTopK() == null ? ragAppProperties.getTopK() : request.getTopK();
        topK = Math.max(1, Math.min(20, topK));

        List<float[]> vectors = embeddingClient.embed(embeddingModel, List.of(question));
        if (vectors.isEmpty()) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "问题向量化失败");
        }

        List<ChunkVectorStore.RetrievedChunk> hits = chunkVectorStore.knnSearch(
                resolved.kbIds(),
                embeddingModel.getId(),
                vectors.get(0),
                topK
        );
        log.debug("RAG retrieve: kbIds={}, hits={}", resolved.kbIds(), hits.size());

        Map<Long, String> titleMap = loadDocumentTitles(hits);
        List<RagCitation> citations = hits.isEmpty() ? List.of() : toCitations(hits, titleMap);
        String userPrompt = hits.isEmpty() ? question : buildUserPrompt(question, hits, titleMap);
        return new PreparedAsk(
                resolved.kbIds(),
                resolved.kbNames(),
                chatModel,
                hits,
                citations,
                userPrompt,
                RAG_SYSTEM_PROMPT,
                true,
                session,
                question
        );
    }

    private ChatSession resolveSession(RagAskRequest request, LoginUser loginUser) {
        if (request.getSessionId() == null || loginUser == null) {
            return null;
        }
        ChatSession session = chatSessionService.requireOwnedSession(loginUser, request.getSessionId());
        chatSessionService.touchPreferences(session, request.getModelId(), request.getKbIds());
        return session;
    }

    private void persistUserMessage(PreparedAsk prepared) {
        if (prepared.session() == null) {
            return;
        }
        chatSessionService.appendUserMessage(prepared.session(), prepared.userQuestion());
    }

    private void persistAssistantMessage(PreparedAsk prepared, String answer, List<RagCitation> citations) {
        if (prepared.session() == null) {
            return;
        }
        chatSessionService.appendAssistantMessage(prepared.session(), answer, citations);
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

    /**
     * kbIds 为空且无旧字段 kbId → 关闭 RAG。
     * 有选择 → 校验并仅在所选库检索。
     */
    private ResolvedKbs resolveKnowledgeBases(RagAskRequest request) {
        List<Long> raw = new ArrayList<>();
        if (request.getKbIds() != null) {
            for (Long id : request.getKbIds()) {
                if (id != null && id > 0) {
                    raw.add(id);
                }
            }
        }
        if (raw.isEmpty() && request.getKbId() != null && request.getKbId() > 0) {
            raw.add(request.getKbId());
        }
        raw = raw.stream().distinct().collect(Collectors.toCollection(ArrayList::new));

        if (raw.isEmpty()) {
            return new ResolvedKbs(List.of(), List.of(), false);
        }

        List<KnowledgeBase> selected = new ArrayList<>();
        for (Long id : raw) {
            KnowledgeBase kb = knowledgeBaseRepository.findById(id)
                    .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "知识库不存在: " + id));
            if (!Boolean.TRUE.equals(kb.getEnabled())) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "知识库未启用: " + kb.getName());
            }
            selected.add(kb);
        }
        return new ResolvedKbs(
                selected.stream().map(KnowledgeBase::getId).toList(),
                selected.stream().map(KnowledgeBase::getName).toList(),
                true
        );
    }

    /**
     * SSE 以空行分帧。必须先写成单行 JSON，再作为 text 发送：
     * 若把 Map 交给 APPLICATION_JSON，Spring 可能按真实换行拆 data: 行，
     * 纯空白 delta（"\\n" / "\\n\\n"）会被拆没，Markdown 围栏就会粘连。
     */
    private void sendEvent(SseEmitter emitter, String event, Object data) throws IOException {
        String json = objectMapper.writeValueAsString(data);
        emitter.send(SseEmitter.event()
                .name(event)
                .data(json, MediaType.TEXT_PLAIN));
    }

    private Map<Long, String> loadDocumentTitles(List<ChunkVectorStore.RetrievedChunk> hits) {
        Set<Long> ids = new HashSet<>();
        for (ChunkVectorStore.RetrievedChunk hit : hits) {
            if (hit.documentId() != null) {
                ids.add(hit.documentId());
            }
        }
        Map<Long, String> map = new HashMap<>();
        if (ids.isEmpty()) {
            return map;
        }
        for (KbDocument doc : kbDocumentRepository.findAllById(ids)) {
            map.put(doc.getId(), doc.getTitle());
        }
        return map;
    }

    private List<RagCitation> toCitations(List<ChunkVectorStore.RetrievedChunk> hits,
                                          Map<Long, String> titleMap) {
        int preview = Math.max(40, ragAppProperties.getCitationPreviewChars());
        List<RagCitation> list = new ArrayList<>(hits.size());
        for (int i = 0; i < hits.size(); i++) {
            ChunkVectorStore.RetrievedChunk hit = hits.get(i);
            String title = titleMap.getOrDefault(hit.documentId(), "文档#" + hit.documentId());
            list.add(new RagCitation(
                    i + 1,
                    hit.chunkId(),
                    hit.documentId(),
                    title,
                    hit.chunkIndex(),
                    truncate(hit.content(), preview),
                    roundScore(hit.score())
            ));
        }
        return list;
    }

    private String buildUserPrompt(String question,
                                   List<ChunkVectorStore.RetrievedChunk> hits,
                                   Map<Long, String> titleMap) {
        int budget = Math.max(500, ragAppProperties.getMaxContextChars());
        StringBuilder refs = new StringBuilder();
        int used = 0;
        for (int i = 0; i < hits.size(); i++) {
            ChunkVectorStore.RetrievedChunk hit = hits.get(i);
            String title = titleMap.getOrDefault(hit.documentId(), "文档#" + hit.documentId());
            String header = String.format(Locale.ROOT,
                    "[%d] 文档《%s》片段#%s（相关度=%.4f）%n",
                    i + 1,
                    title,
                    hit.chunkIndex() == null ? "?" : hit.chunkIndex(),
                    hit.score());
            String body = hit.content() == null ? "" : hit.content().trim();
            int cost = header.length() + body.length() + 2;
            if (used > 0 && used + cost > budget) {
                break;
            }
            refs.append(header).append(body).append("\n\n");
            used += cost;
        }

        return "问题：\n" + question + "\n\n参考资料：\n" + refs;
    }

    private static String truncate(String text, int max) {
        if (text == null) {
            return "";
        }
        String value = text.trim();
        if (value.length() <= max) {
            return value;
        }
        return value.substring(0, max) + "…";
    }

    private static double roundScore(double score) {
        return Math.round(score * 10000d) / 10000d;
    }

    private record ResolvedKbs(List<Long> kbIds, List<String> kbNames, boolean ragEnabled) {
    }

    private record PreparedAsk(List<Long> kbIds,
                               List<String> kbNames,
                               LlmModel chatModel,
                               List<ChunkVectorStore.RetrievedChunk> hits,
                               List<RagCitation> citations,
                               String userPrompt,
                               String systemPrompt,
                               boolean ragEnabled,
                               ChatSession session,
                               String userQuestion) {
    }
}
