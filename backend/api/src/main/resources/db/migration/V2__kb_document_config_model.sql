-- Day3: 知识库、文档、系统配置、模型
CREATE TABLE IF NOT EXISTS knowledge_base (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    name        VARCHAR(128) NOT NULL,
    description VARCHAR(512) NULL,
    enabled     TINYINT(1)   NOT NULL DEFAULT 1,
    created_at  DATETIME     NOT NULL,
    updated_at  DATETIME     NOT NULL,
    UNIQUE KEY uk_kb_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS kb_document (
    id                BIGINT PRIMARY KEY AUTO_INCREMENT,
    kb_id             BIGINT       NOT NULL,
    title             VARCHAR(256) NOT NULL,
    original_filename VARCHAR(256) NOT NULL,
    object_key        VARCHAR(512) NOT NULL,
    content_type      VARCHAR(128) NULL,
    file_size         BIGINT       NOT NULL DEFAULT 0,
    status            VARCHAR(32)  NOT NULL,
    error_message     VARCHAR(1024) NULL,
    created_at        DATETIME     NOT NULL,
    updated_at        DATETIME     NOT NULL,
    KEY idx_doc_kb_id (kb_id),
    KEY idx_doc_status (status),
    CONSTRAINT fk_doc_kb FOREIGN KEY (kb_id) REFERENCES knowledge_base (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sys_config (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    config_key  VARCHAR(128)  NOT NULL,
    config_value TEXT         NULL,
    remark      VARCHAR(256)  NULL,
    created_at  DATETIME      NOT NULL,
    updated_at  DATETIME      NOT NULL,
    UNIQUE KEY uk_sys_config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS llm_model (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    name        VARCHAR(128)  NOT NULL,
    protocol    VARCHAR(32)   NOT NULL,
    base_url    VARCHAR(512)  NOT NULL,
    api_key     VARCHAR(512)  NOT NULL,
    model_name  VARCHAR(128)  NULL,
    enabled     TINYINT(1)    NOT NULL DEFAULT 1,
    remark      VARCHAR(256)  NULL,
    created_at  DATETIME      NOT NULL,
    updated_at  DATETIME      NOT NULL,
    UNIQUE KEY uk_llm_model_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO sys_config (config_key, config_value, remark, created_at, updated_at) VALUES
('site_name', '企业知识库智能问答', '站点名称', NOW(), NOW()),
('site_description', '基于 RAG 的企业内部知识问答系统', '站点描述', NOW(), NOW()),
('site_logo', '', '站点 Logo URL', NOW(), NOW()),
('contact_email', '', '联系邮箱', NOW(), NOW());
