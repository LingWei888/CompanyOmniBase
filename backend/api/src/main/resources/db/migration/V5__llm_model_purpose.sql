-- llm_model 增加用途：CHAT（对话）/ EMBEDDING（向量化）
ALTER TABLE llm_model
    ADD COLUMN purpose VARCHAR(32) NOT NULL DEFAULT 'CHAT' AFTER protocol;

UPDATE llm_model SET purpose = 'CHAT' WHERE purpose IS NULL OR purpose = '';
