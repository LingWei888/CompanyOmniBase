package cn.exitcode.richpeasants.rag.service;

import cn.exitcode.richpeasants.common.entity.ChatMessage;
import cn.exitcode.richpeasants.common.entity.ChatSession;
import cn.exitcode.richpeasants.common.exception.BusinessException;
import cn.exitcode.richpeasants.common.repository.ChatMessageRepository;
import cn.exitcode.richpeasants.common.repository.ChatSessionRepository;
import cn.exitcode.richpeasants.common.result.ResultCode;
import cn.exitcode.richpeasants.common.security.LoginUser;
import cn.exitcode.richpeasants.rag.dto.ChatMessageItemResponse;
import cn.exitcode.richpeasants.rag.dto.ChatSessionCreateRequest;
import cn.exitcode.richpeasants.rag.dto.ChatSessionDetailResponse;
import cn.exitcode.richpeasants.rag.dto.ChatSessionItemResponse;
import cn.exitcode.richpeasants.rag.dto.ChatSessionUpdateRequest;
import cn.exitcode.richpeasants.rag.dto.RagCitation;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ChatSessionService {

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ObjectMapper objectMapper;

    public ChatSessionService(ChatSessionRepository chatSessionRepository,
                              ChatMessageRepository chatMessageRepository,
                              ObjectMapper objectMapper) {
        this.chatSessionRepository = chatSessionRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.objectMapper = objectMapper;
    }

    public List<ChatSessionItemResponse> list(LoginUser user) {
        requireUser(user);
        return chatSessionRepository.findByUserIdOrderByUpdatedAtDesc(user.getUserId()).stream()
                .map(this::toItem)
                .collect(Collectors.toList());
    }

    @Transactional
    public ChatSessionItemResponse create(LoginUser user, ChatSessionCreateRequest request) {
        requireUser(user);
        // 已有空会话（尚无消息）时复用，避免多个「新对话」
        List<ChatSession> existing = chatSessionRepository.findByUserIdOrderByUpdatedAtDesc(user.getUserId());
        for (ChatSession session : existing) {
            if (!chatMessageRepository.existsBySessionId(session.getId())) {
                if (request.getModelId() != null) {
                    session.setModelId(request.getModelId());
                }
                if (request.getKbIds() != null) {
                    session.setKbIdsJson(encodeKbIds(request.getKbIds()));
                }
                if (!StringUtils.hasText(session.getTitle())) {
                    session.setTitle("新对话");
                }
                session.setUpdatedAt(java.time.LocalDateTime.now());
                return toItem(chatSessionRepository.save(session));
            }
        }
        ChatSession session = new ChatSession();
        session.setUserId(user.getUserId());
        session.setTitle(StringUtils.hasText(request.getTitle()) ? request.getTitle().trim() : "新对话");
        session.setModelId(request.getModelId());
        session.setKbIdsJson(encodeKbIds(request.getKbIds()));
        ChatSession saved = chatSessionRepository.save(session);
        return toItem(saved);
    }

    public ChatSessionDetailResponse detail(LoginUser user, Long sessionId) {
        ChatSession session = requireOwnedSession(user, sessionId);
        ChatSessionDetailResponse response = new ChatSessionDetailResponse();
        response.setId(session.getId());
        response.setTitle(session.getTitle());
        response.setModelId(session.getModelId());
        response.setKbIds(decodeKbIds(session.getKbIdsJson()));
        response.setMessages(chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId).stream()
                .map(this::toMessageItem)
                .collect(Collectors.toList()));
        return response;
    }

    @Transactional
    public ChatSessionItemResponse update(LoginUser user, Long sessionId, ChatSessionUpdateRequest request) {
        ChatSession session = requireOwnedSession(user, sessionId);
        if (StringUtils.hasText(request.getTitle())) {
            session.setTitle(request.getTitle().trim());
        }
        if (request.getKbIds() != null) {
            session.setKbIdsJson(encodeKbIds(request.getKbIds()));
        }
        if (request.getModelId() != null) {
            session.setModelId(request.getModelId());
        }
        session.setUpdatedAt(java.time.LocalDateTime.now());
        return toItem(chatSessionRepository.save(session));
    }

    @Transactional
    public void delete(LoginUser user, Long sessionId) {
        ChatSession session = requireOwnedSession(user, sessionId);
        chatMessageRepository.deleteBySessionId(sessionId);
        chatSessionRepository.delete(session);
    }

    @Transactional
    public ChatSession requireOwnedSession(LoginUser user, Long sessionId) {
        requireUser(user);
        if (sessionId == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "会话 ID 无效");
        }
        return chatSessionRepository.findByIdAndUserId(sessionId, user.getUserId())
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "会话不存在"));
    }

    @Transactional
    public void appendUserMessage(ChatSession session, String content) {
        saveMessage(session.getId(), "user", content, null);
        touchSession(session, content);
    }

    @Transactional
    public void appendAssistantMessage(ChatSession session, String content, List<RagCitation> citations) {
        saveMessage(session.getId(), "assistant", content, citations);
        touchSession(session, null);
    }

    @Transactional
    public void touchPreferences(ChatSession session, Long modelId, List<Long> kbIds) {
        if (modelId != null) {
            session.setModelId(modelId);
        }
        if (kbIds != null) {
            session.setKbIdsJson(encodeKbIds(kbIds));
        }
        session.setUpdatedAt(java.time.LocalDateTime.now());
        chatSessionRepository.save(session);
    }

    private void saveMessage(Long sessionId, String role, String content, List<RagCitation> citations) {
        ChatMessage message = new ChatMessage();
        message.setSessionId(sessionId);
        message.setRole(role);
        message.setContent(content == null ? "" : content);
        if (citations != null && !citations.isEmpty()) {
            try {
                message.setCitationsJson(objectMapper.writeValueAsString(citations));
            } catch (JsonProcessingException ex) {
                message.setCitationsJson("[]");
            }
        }
        chatMessageRepository.save(message);
    }

    private void touchSession(ChatSession session, String firstUserContent) {
        if ("新对话".equals(session.getTitle()) && StringUtils.hasText(firstUserContent)) {
            String title = firstUserContent.trim();
            if (title.length() > 24) {
                title = title.substring(0, 24);
            }
            session.setTitle(title);
        }
        session.setUpdatedAt(java.time.LocalDateTime.now());
        chatSessionRepository.save(session);
    }

    private ChatSessionItemResponse toItem(ChatSession session) {
        return new ChatSessionItemResponse(
                session.getId(),
                session.getTitle(),
                session.getModelId(),
                decodeKbIds(session.getKbIdsJson()),
                session.getUpdatedAt()
        );
    }

    private ChatMessageItemResponse toMessageItem(ChatMessage message) {
        ChatMessageItemResponse item = new ChatMessageItemResponse();
        item.setId(message.getId());
        item.setRole(message.getRole());
        item.setContent(message.getContent());
        item.setCreatedAt(message.getCreatedAt());
        item.setCitations(decodeCitations(message.getCitationsJson()));
        return item;
    }

    private String encodeKbIds(List<Long> kbIds) {
        if (kbIds == null || kbIds.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(kbIds);
        } catch (JsonProcessingException ex) {
            return "[]";
        }
    }

    private List<Long> decodeKbIds(String json) {
        if (!StringUtils.hasText(json)) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<Long>>() {
            });
        } catch (JsonProcessingException ex) {
            return new ArrayList<>();
        }
    }

    private List<RagCitation> decodeCitations(String json) {
        if (!StringUtils.hasText(json)) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<RagCitation>>() {
            });
        } catch (JsonProcessingException ex) {
            return new ArrayList<>();
        }
    }

    private void requireUser(LoginUser user) {
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        }
    }
}
