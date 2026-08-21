-- 用户长期记忆（事实元数据在 MySQL；向量在 ES user_memory_vector）
CREATE TABLE user_memory (
    id                 BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id            BIGINT       NOT NULL,
    content            VARCHAR(1000) NOT NULL,
    category           VARCHAR(32)  NULL,
    source_session_id  BIGINT       NULL,
    importance         TINYINT      NOT NULL DEFAULT 1,
    embedding_model_id BIGINT       NULL,
    created_at         DATETIME     NOT NULL,
    updated_at         DATETIME     NOT NULL,
    last_used_at       DATETIME     NULL,
    INDEX idx_user_memory_user_updated (user_id, updated_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
