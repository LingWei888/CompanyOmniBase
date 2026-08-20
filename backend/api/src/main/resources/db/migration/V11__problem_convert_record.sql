-- 题意修改智能体：转换记录持久化
CREATE TABLE problem_convert_record (
    id                   BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id              BIGINT       NOT NULL,
    title                VARCHAR(200) NOT NULL DEFAULT '未命名转换',
    reference_nickname   VARCHAR(200) NOT NULL DEFAULT '',
    original_text        LONGTEXT     NOT NULL,
    result_markdown      LONGTEXT     NOT NULL,
    created_at           DATETIME     NOT NULL,
    updated_at           DATETIME     NOT NULL,
    INDEX idx_pcr_user_updated (user_id, updated_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
