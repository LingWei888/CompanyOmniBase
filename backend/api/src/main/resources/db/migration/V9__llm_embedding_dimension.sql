-- Embedding 模型向量维度（与 ES dense_vector.dims 对齐）
ALTER TABLE llm_model
    ADD COLUMN embedding_dimension INT NULL AFTER model_name;

UPDATE llm_model
SET embedding_dimension = 1536
WHERE purpose = 'EMBEDDING' AND embedding_dimension IS NULL;
