-- Day9：聊天会话与消息持久化
CREATE TABLE chat_session (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT       NOT NULL,
    title       VARCHAR(200) NOT NULL DEFAULT '新对话',
    model_id    BIGINT       NULL,
    kb_ids_json VARCHAR(512) NULL,
    created_at  DATETIME     NOT NULL,
    updated_at  DATETIME     NOT NULL,
    INDEX idx_chat_session_user_updated (user_id, updated_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE chat_message (
    id             BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    session_id     BIGINT       NOT NULL,
    role           VARCHAR(16)  NOT NULL,
    content        LONGTEXT     NOT NULL,
    citations_json LONGTEXT     NULL,
    created_at     DATETIME     NOT NULL,
    INDEX idx_chat_message_session (session_id, created_at ASC),
    CONSTRAINT fk_chat_message_session FOREIGN KEY (session_id) REFERENCES chat_session (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
