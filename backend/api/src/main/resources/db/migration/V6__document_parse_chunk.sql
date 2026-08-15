-- Day5：解析/切分进度细化 + 切分参数 + 解析正文与片段表
UPDATE kb_document SET status = 'PENDING' WHERE status = 'PROCESSING';

ALTER TABLE kb_document
    ADD COLUMN chunk_size INT NOT NULL DEFAULT 800 AFTER error_message,
    ADD COLUMN chunk_overlap INT NOT NULL DEFAULT 100 AFTER chunk_size,
    ADD COLUMN parsed_char_count INT NOT NULL DEFAULT 0 AFTER chunk_overlap,
    ADD COLUMN chunk_count INT NOT NULL DEFAULT 0 AFTER parsed_char_count;

CREATE TABLE IF NOT EXISTS kb_document_parsed (
    document_id BIGINT PRIMARY KEY,
    content     LONGTEXT NOT NULL,
    updated_at  DATETIME NOT NULL,
    CONSTRAINT fk_doc_parsed_doc FOREIGN KEY (document_id) REFERENCES kb_document (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS kb_document_chunk (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    document_id  BIGINT       NOT NULL,
    kb_id        BIGINT       NOT NULL,
    chunk_index  INT          NOT NULL,
    content      TEXT         NOT NULL,
    char_count   INT          NOT NULL DEFAULT 0,
    created_at   DATETIME     NOT NULL,
    UNIQUE KEY uk_doc_chunk (document_id, chunk_index),
    KEY idx_chunk_doc (document_id),
    KEY idx_chunk_kb (kb_id),
    CONSTRAINT fk_chunk_doc FOREIGN KEY (document_id) REFERENCES kb_document (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO sys_config (config_key, config_value, remark, created_at, updated_at)
SELECT 'ingest_chunk_size', '800', '默认切分长度（字符）', NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'ingest_chunk_size');

INSERT INTO sys_config (config_key, config_value, remark, created_at, updated_at)
SELECT 'ingest_chunk_overlap', '100', '默认切分重叠长度（字符）', NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'ingest_chunk_overlap');
