-- 知识库可选默认切分参数（NULL = 使用系统默认）
ALTER TABLE knowledge_base
    ADD COLUMN default_chunk_size INT NULL AFTER enabled,
    ADD COLUMN default_chunk_overlap INT NULL AFTER default_chunk_size;
