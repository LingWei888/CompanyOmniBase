-- 数据生成智能体：生成记录持久化
CREATE TABLE testdata_gen_record (
    id                   BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id              BIGINT       NOT NULL,
    title                VARCHAR(200) NOT NULL DEFAULT '未命名生成',
    original_text        LONGTEXT     NOT NULL,
    result_python        LONGTEXT     NOT NULL,
    solution_code        LONGTEXT     NULL,
    created_at           DATETIME     NOT NULL,
    updated_at           DATETIME     NOT NULL,
    INDEX idx_tgr_user_updated (user_id, updated_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
