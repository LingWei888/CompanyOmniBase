package cn.exitcode.richpeasants.common.repository;

import cn.exitcode.richpeasants.common.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findBySessionIdOrderByCreatedAtAsc(Long sessionId);

    void deleteBySessionId(Long sessionId);

    boolean existsBySessionId(Long sessionId);
}
