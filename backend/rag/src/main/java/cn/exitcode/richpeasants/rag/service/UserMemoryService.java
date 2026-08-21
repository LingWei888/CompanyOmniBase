package cn.exitcode.richpeasants.rag.service;

import cn.exitcode.richpeasants.common.entity.LlmModel;
import cn.exitcode.richpeasants.common.entity.UserMemory;
import cn.exitcode.richpeasants.common.exception.BusinessException;
import cn.exitcode.richpeasants.common.repository.UserMemoryRepository;
import cn.exitcode.richpeasants.common.result.ResultCode;
import cn.exitcode.richpeasants.common.security.LoginUser;
import cn.exitcode.richpeasants.ingest.embedding.EmbeddingClient;
import cn.exitcode.richpeasants.rag.config.RagAppProperties;
import cn.exitcode.richpeasants.rag.config.RagAsyncConfig;
import cn.exitcode.richpeasants.rag.dto.UserMemoryItemResponse;
import cn.exitcode.richpeasants.rag.llm.ChatCompletionsClient;
import cn.exitcode.richpeasants.rag.memory.MemoryVectorStore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserMemoryService {

    private static final Logger log = LoggerFactory.getLogger(UserMemoryService.class);

    private static final String EXTRACT_SYSTEM_PROMPT = """
            你是用户画像抽取器。根据本轮对话，抽出「值得跨会话长期记住」的原子事实。
            只输出 JSON 数组，不要 Markdown，不要解释。每项字段：
            - content: 简洁中文陈述（第三人称或客观事实，如「用户偏好用中文回答」）
            - category: preference / fact / instruction 之一
            - importance: 1~5 整数
            规则：
            1. 无价值则输出 []
            2. 不要记临时天气/新闻/单次查询结果
            3. 不要记知识库文档内容本身
            4. 单条 content 不超过 120 字；最多 3 条
            """;

    private final UserMemoryRepository userMemoryRepository;
    private final MemoryVectorStore memoryVectorStore;
    private final EmbeddingClient embeddingClient;
    private final ChatCompletionsClient chatCompletionsClient;
    private final RagAppProperties ragAppProperties;
    private final ObjectMapper objectMapper;
    private final UserMemoryService self;

    public UserMemoryService(UserMemoryRepository userMemoryRepository,
                             MemoryVectorStore memoryVectorStore,
                             EmbeddingClient embeddingClient,
                             ChatCompletionsClient chatCompletionsClient,
                             RagAppProperties ragAppProperties,
                             ObjectMapper objectMapper,
                             @Lazy UserMemoryService self) {
        this.userMemoryRepository = userMemoryRepository;
        this.memoryVectorStore = memoryVectorStore;
        this.embeddingClient = embeddingClient;
        this.chatCompletionsClient = chatCompletionsClient;
        this.ragAppProperties = ragAppProperties;
        this.objectMapper = objectMapper;
        this.self = self;
    }

    /**
     * 按当前问题向量检索用户长期记忆，返回可注入 system 的条目文本。
     */
    @Transactional
    public List<String> retrieveForPrompt(Long userId, String question) {
        RagAppProperties.LongTerm cfg = ragAppProperties.getMemory().getLongTerm();
        if (!cfg.isEnabled() || userId == null || !StringUtils.hasText(question)) {
            return List.of();
        }
        try {
            if (userMemoryRepository.countByUserId(userId) == 0) {
                return List.of();
            }
            LlmModel embeddingModel = embeddingClient.requireEmbeddingModel();
            Integer dims = embeddingModel.getEmbeddingDimension();
            if (dims == null || dims <= 0) {
                log.warn("Embedding model missing dimension, skip long-term retrieve");
                return List.of();
            }
            memoryVectorStore.ensureIndex(dims);
            List<float[]> vectors = embeddingClient.embed(embeddingModel, List.of(question.trim()));
            if (vectors.isEmpty()) {
                return List.of();
            }
            int topK = Math.max(1, Math.min(20, cfg.getTopK()));
            List<MemoryVectorStore.RetrievedMemory> hits = memoryVectorStore.knnSearch(
                    userId, embeddingModel.getId(), vectors.get(0), topK);
            if (hits.isEmpty()) {
                return List.of();
            }

            List<Long> ids = hits.stream()
                    .filter(h -> h.score() >= cfg.getMinScore())
                    .map(MemoryVectorStore.RetrievedMemory::memoryId)
                    .filter(id -> id != null)
                    .toList();
            if (ids.isEmpty()) {
                return List.of();
            }

            List<UserMemory> rows = userMemoryRepository.findByUserIdAndIdIn(userId, ids);
            var byId = rows.stream().collect(Collectors.toMap(UserMemory::getId, m -> m, (a, b) -> a));
            LocalDateTime now = LocalDateTime.now();
            List<String> texts = new ArrayList<>();
            int budget = Math.max(200, cfg.getMaxInjectChars());
            int used = 0;
            for (MemoryVectorStore.RetrievedMemory hit : hits) {
                if (hit.score() < cfg.getMinScore() || hit.memoryId() == null) {
                    continue;
                }
                UserMemory memory = byId.get(hit.memoryId());
                String content = memory != null ? memory.getContent() : hit.content();
                if (!StringUtils.hasText(content)) {
                    continue;
                }
                int cost = content.length() + 2;
                if (used > 0 && used + cost > budget) {
                    break;
                }
                texts.add(content.trim());
                used += cost;
                if (memory != null) {
                    memory.setLastUsedAt(now);
                }
            }
            if (!rows.isEmpty()) {
                userMemoryRepository.saveAll(rows);
            }
            return texts;
        } catch (BusinessException ex) {
            log.warn("Long-term memory retrieve skipped: {}", ex.getMessage());
            return List.of();
        } catch (Exception ex) {
            log.warn("Long-term memory retrieve failed: {}", ex.getMessage());
            return List.of();
        }
    }

    public String appendToSystemPrompt(String baseSystem, List<String> memories) {
        if (memories == null || memories.isEmpty()) {
            return baseSystem;
        }
        StringBuilder block = new StringBuilder();
        block.append("\n\n【用户长期记忆】（跨会话背景；与本轮参考资料冲突时以参考资料为准）\n");
        for (String item : memories) {
            block.append("- ").append(item).append('\n');
        }
        return (baseSystem == null ? "" : baseSystem) + block;
    }

    @Async(RagAsyncConfig.MEMORY_EXECUTOR)
    public void extractAndStoreAsync(Long userId,
                                     Long sessionId,
                                     String question,
                                     String answer,
                                     LlmModel chatModel) {
        try {
            // 经代理调用，保证 @Transactional 生效
            self.extractAndStore(userId, sessionId, question, answer, chatModel);
        } catch (Exception ex) {
            log.warn("Long-term memory extract failed userId={}: {}", userId, ex.getMessage());
        }
    }

    @Transactional
    public void extractAndStore(Long userId,
                                Long sessionId,
                                String question,
                                String answer,
                                LlmModel chatModel) {
        RagAppProperties.LongTerm cfg = ragAppProperties.getMemory().getLongTerm();
        if (!cfg.isEnabled() || !cfg.isExtractEnabled() || userId == null || chatModel == null) {
            return;
        }
        if (!StringUtils.hasText(question) || !StringUtils.hasText(answer)) {
            return;
        }

        String userPrompt = "用户：" + question.trim() + "\n助手：" + truncate(answer.trim(), 2000);
        String raw = chatCompletionsClient.complete(chatModel, EXTRACT_SYSTEM_PROMPT, userPrompt, 0.1);
        List<ExtractedFact> facts = parseFacts(raw);
        if (facts.isEmpty()) {
            return;
        }

        LlmModel embeddingModel = embeddingClient.requireEmbeddingModel();
        Integer dims = embeddingModel.getEmbeddingDimension();
        if (dims == null || dims <= 0) {
            log.warn("Embedding model missing dimension, skip long-term extract");
            return;
        }
        memoryVectorStore.ensureIndex(dims);

        for (ExtractedFact fact : facts) {
            saveIfNovel(userId, sessionId, fact, embeddingModel, cfg);
        }
        enforceQuota(userId, cfg.getMaxPerUser());
    }

    private void saveIfNovel(Long userId,
                             Long sessionId,
                             ExtractedFact fact,
                             LlmModel embeddingModel,
                             RagAppProperties.LongTerm cfg) {
        List<float[]> vectors = embeddingClient.embed(embeddingModel, List.of(fact.content()));
        if (vectors.isEmpty()) {
            return;
        }
        float[] embedding = vectors.get(0);
        List<MemoryVectorStore.RetrievedMemory> similar = memoryVectorStore.knnSearch(
                userId, embeddingModel.getId(), embedding, 3);
        for (MemoryVectorStore.RetrievedMemory hit : similar) {
            if (hit.score() >= cfg.getDuplicateThreshold()) {
                log.debug("Skip duplicate memory score={}: {}", hit.score(), fact.content());
                return;
            }
        }

        UserMemory memory = new UserMemory();
        memory.setUserId(userId);
        memory.setContent(fact.content());
        memory.setCategory(fact.category());
        memory.setImportance(fact.importance());
        memory.setSourceSessionId(sessionId);
        memory.setEmbeddingModelId(embeddingModel.getId());
        UserMemory saved = userMemoryRepository.save(memory);
        memoryVectorStore.indexMemory(
                saved.getId(),
                userId,
                embeddingModel.getId(),
                saved.getContent(),
                saved.getCategory(),
                embedding
        );
        log.info("Saved long-term memory id={} userId={} category={}",
                saved.getId(), userId, fact.category());
    }

    private void enforceQuota(Long userId, int maxPerUser) {
        int limit = Math.max(10, maxPerUser);
        List<UserMemory> all = userMemoryRepository.findByUserIdOrderByUpdatedAtDesc(userId);
        if (all.size() <= limit) {
            return;
        }
        List<UserMemory> overflow = all.subList(limit, all.size());
        for (UserMemory memory : overflow) {
            memoryVectorStore.deleteMemory(memory.getId());
        }
        userMemoryRepository.deleteAll(overflow);
    }

    public List<UserMemoryItemResponse> list(LoginUser user) {
        requireUser(user);
        return userMemoryRepository.findByUserIdOrderByUpdatedAtDesc(user.getUserId()).stream()
                .map(this::toItem)
                .collect(Collectors.toList());
    }

    @Transactional
    public void delete(LoginUser user, Long memoryId) {
        requireUser(user);
        UserMemory memory = userMemoryRepository.findByIdAndUserId(memoryId, user.getUserId())
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "记忆不存在"));
        memoryVectorStore.deleteMemory(memory.getId());
        userMemoryRepository.delete(memory);
    }

    @Transactional
    public void clearAll(LoginUser user) {
        requireUser(user);
        List<UserMemory> all = userMemoryRepository.findByUserIdOrderByUpdatedAtDesc(user.getUserId());
        memoryVectorStore.deleteByUserId(user.getUserId());
        userMemoryRepository.deleteAll(all);
    }

    private List<ExtractedFact> parseFacts(String raw) {
        String json = extractJsonArray(raw);
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            JsonNode root = objectMapper.readTree(json);
            if (!root.isArray()) {
                return List.of();
            }
            Set<String> seen = new LinkedHashSet<>();
            List<ExtractedFact> facts = new ArrayList<>();
            for (JsonNode node : root) {
                String content = node.path("content").asText("").trim();
                if (!StringUtils.hasText(content) || content.length() < 4) {
                    continue;
                }
                if (content.length() > 200) {
                    content = content.substring(0, 200);
                }
                String key = content.toLowerCase(Locale.ROOT);
                if (!seen.add(key)) {
                    continue;
                }
                String category = normalizeCategory(node.path("category").asText("fact"));
                int importance = node.path("importance").asInt(1);
                importance = Math.max(1, Math.min(5, importance));
                facts.add(new ExtractedFact(content, category, importance));
                if (facts.size() >= 3) {
                    break;
                }
            }
            facts.sort(Comparator.comparingInt(ExtractedFact::importance).reversed());
            return facts;
        } catch (Exception ex) {
            log.debug("Parse memory extract JSON failed: {}", ex.getMessage());
            return List.of();
        }
    }

    private static String extractJsonArray(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        String text = raw.trim();
        if (text.startsWith("```")) {
            int start = text.indexOf('[');
            int end = text.lastIndexOf(']');
            if (start >= 0 && end > start) {
                return text.substring(start, end + 1);
            }
        }
        int start = text.indexOf('[');
        int end = text.lastIndexOf(']');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return null;
    }

    private static String normalizeCategory(String raw) {
        if (!StringUtils.hasText(raw)) {
            return "fact";
        }
        String value = raw.trim().toLowerCase(Locale.ROOT);
        return switch (value) {
            case "preference", "instruction", "fact" -> value;
            default -> "fact";
        };
    }

    private UserMemoryItemResponse toItem(UserMemory memory) {
        UserMemoryItemResponse item = new UserMemoryItemResponse();
        item.setId(memory.getId());
        item.setContent(memory.getContent());
        item.setCategory(memory.getCategory());
        item.setImportance(memory.getImportance());
        item.setCreatedAt(memory.getCreatedAt());
        item.setUpdatedAt(memory.getUpdatedAt());
        item.setLastUsedAt(memory.getLastUsedAt());
        return item;
    }

    private void requireUser(LoginUser user) {
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        }
    }

    private static String truncate(String text, int max) {
        if (text == null) {
            return "";
        }
        if (text.length() <= max) {
            return text;
        }
        return text.substring(0, max) + "…";
    }

    private record ExtractedFact(String content, String category, int importance) {
    }
}
