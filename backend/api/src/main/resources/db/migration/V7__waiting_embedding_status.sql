-- 切分完成后进入等待 Embedding，不再直接 READY
UPDATE kb_document
SET status = 'WAITING_EMBEDDING'
WHERE status = 'READY';
