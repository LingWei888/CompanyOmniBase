package cn.exitcode.richpeasants.ingest.service;

import cn.exitcode.richpeasants.common.entity.KbDocument;
import cn.exitcode.richpeasants.common.entity.KbDocumentChunk;
import cn.exitcode.richpeasants.common.entity.KbDocumentParsed;
import cn.exitcode.richpeasants.common.entity.LlmModel;
import cn.exitcode.richpeasants.common.enums.DocumentStatus;
import cn.exitcode.richpeasants.common.repository.KbDocumentChunkRepository;
import cn.exitcode.richpeasants.common.repository.KbDocumentParsedRepository;
import cn.exitcode.richpeasants.common.repository.KbDocumentRepository;
import cn.exitcode.richpeasants.common.storage.MinioStorageService;
import cn.exitcode.richpeasants.ingest.chunk.TextChunker;
import cn.exitcode.richpeasants.ingest.config.IngestAppProperties;
import cn.exitcode.richpeasants.ingest.embedding.EmbeddingClient;
import cn.exitcode.richpeasants.ingest.es.ChunkVectorStore;
import cn.exitcode.richpeasants.ingest.mq.DocumentIngestMessage;
import cn.exitcode.richpeasants.ingest.parse.DocumentTextExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class DocumentIngestService {

    private static final Logger log = LoggerFactory.getLogger(DocumentIngestService.class);

    private final KbDocumentRepository kbDocumentRepository;
    private final KbDocumentParsedRepository kbDocumentParsedRepository;
    private final KbDocumentChunkRepository kbDocumentChunkRepository;
    private final MinioStorageService minioStorageService;
    private final DocumentTextExtractor documentTextExtractor;
    private final TextChunker textChunker;
    private final EmbeddingClient embeddingClient;
    private final ChunkVectorStore chunkVectorStore;
    private final IngestAppProperties ingestAppProperties;
    private final TransactionTemplate transactionTemplate;

    public DocumentIngestService(KbDocumentRepository kbDocumentRepository,
                                 KbDocumentParsedRepository kbDocumentParsedRepository,
                                 KbDocumentChunkRepository kbDocumentChunkRepository,
                                 MinioStorageService minioStorageService,
                                 DocumentTextExtractor documentTextExtractor,
                                 TextChunker textChunker,
                                 EmbeddingClient embeddingClient,
                                 ChunkVectorStore chunkVectorStore,
                                 IngestAppProperties ingestAppProperties,
                                 TransactionTemplate transactionTemplate) {
        this.kbDocumentRepository = kbDocumentRepository;
        this.kbDocumentParsedRepository = kbDocumentParsedRepository;
        this.kbDocumentChunkRepository = kbDocumentChunkRepository;
        this.minioStorageService = minioStorageService;
        this.documentTextExtractor = documentTextExtractor;
        this.textChunker = textChunker;
        this.embeddingClient = embeddingClient;
        this.chunkVectorStore = chunkVectorStore;
        this.ingestAppProperties = ingestAppProperties;
        this.transactionTemplate = transactionTemplate;
    }

    /**
     * 解析 → 切分 → Embedding → ES → READY。
     * 若已是 WAITING_EMBEDDING，则只跑向量化。
     */
    public void handleIngestMessage(DocumentIngestMessage message) {
        if (message == null || message.getDocumentId() == null) {
            log.warn("Ignore empty ingest message");
            return;
        }
        Long documentId = message.getDocumentId();
        Optional<KbDocument> optional = kbDocumentRepository.findById(documentId);
        if (optional.isEmpty()) {
            log.warn("Document not found for ingest: {}", documentId);
            return;
        }
        KbDocument snapshot = optional.get();
        if (snapshot.getStatus() == DocumentStatus.READY) {
            log.info("Skip already READY document: {}", documentId);
            return;
        }
        if (snapshot.getStatus() == DocumentStatus.PARSING
                || snapshot.getStatus() == DocumentStatus.CHUNKING
                || snapshot.getStatus() == DocumentStatus.EMBEDDING) {
            log.info("Document already in progress ({}), skip duplicate message: {}", snapshot.getStatus(), documentId);
            return;
        }

        try {
            if (snapshot.getStatus() == DocumentStatus.WAITING_EMBEDDING) {
                runEmbedding(documentId);
                return;
            }

            markParsing(documentId);

            KbDocument document = kbDocumentRepository.findById(documentId)
                    .orElseThrow(() -> new IllegalStateException("文档不存在: " + documentId));

            byte[] bytes = minioStorageService.downloadBytes(document.getObjectKey());
            String text = documentTextExtractor.extract(bytes, document.getOriginalFilename(), document.getContentType());
            saveParsed(document, text);

            markChunking(documentId);

            int chunkSize = document.getChunkSize() == null ? 800 : document.getChunkSize();
            int overlap = document.getChunkOverlap() == null ? 100 : document.getChunkOverlap();
            List<String> pieces = textChunker.chunk(text, chunkSize, overlap);
            saveChunksAndWaitingEmbedding(documentId, document.getKbId(), pieces);

            log.info("Document parse+chunk done. documentId={}, chars={}, chunks={}",
                    documentId, text.length(), pieces.size());

            runEmbedding(documentId);
        } catch (Exception ex) {
            log.error("Document ingest failed: documentId={}", documentId, ex);
            markFailed(documentId, ex.getMessage());
            throw ex instanceof RuntimeException ? (RuntimeException) ex : new RuntimeException(ex);
        }
    }

    private void runEmbedding(Long documentId) {
        markEmbedding(documentId);

        LlmModel model = embeddingClient.requireEmbeddingModel();
        if (model.getEmbeddingDimension() == null || model.getEmbeddingDimension() <= 0) {
            throw new IllegalStateException("Embedding 模型未配置向量维度");
        }
        int dims = model.getEmbeddingDimension();
        chunkVectorStore.ensureIndex(dims);

        List<KbDocumentChunk> chunks = kbDocumentChunkRepository.findByDocumentIdOrderByChunkIndexAsc(documentId);
        KbDocument document = kbDocumentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalStateException("文档不存在: " + documentId));

        chunkVectorStore.deleteByDocumentId(documentId);

        if (chunks.isEmpty()) {
            markReady(documentId);
            log.info("No chunks to embed, mark READY. documentId={}", documentId);
            return;
        }

        int batchSize = Math.max(1, ingestAppProperties.getEmbedding().getBatchSize());
        List<ChunkVectorStore.ChunkVectorRecord> buffer = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i += batchSize) {
            List<KbDocumentChunk> batch = chunks.subList(i, Math.min(i + batchSize, chunks.size()));
            List<String> texts = batch.stream().map(KbDocumentChunk::getContent).toList();
            List<float[]> vectors = embeddingClient.embed(model, texts);
            for (int j = 0; j < batch.size(); j++) {
                KbDocumentChunk chunk = batch.get(j);
                buffer.add(new ChunkVectorStore.ChunkVectorRecord(
                        chunk.getId(),
                        chunk.getChunkIndex(),
                        chunk.getContent(),
                        vectors.get(j)
                ));
            }
        }
        chunkVectorStore.indexChunks(documentId, document.getKbId(), model.getId(), buffer);
        markReady(documentId);
        log.info("Document embedding done. documentId={}, chunks={}, dims={}", documentId, chunks.size(), dims);
    }

    private void markParsing(Long documentId) {
        transactionTemplate.executeWithoutResult(status -> {
            KbDocument document = kbDocumentRepository.findById(documentId).orElse(null);
            if (document == null) {
                return;
            }
            document.setStatus(DocumentStatus.PARSING);
            document.setErrorMessage(null);
            document.setParsedCharCount(0);
            document.setChunkCount(0);
            kbDocumentRepository.save(document);
            kbDocumentChunkRepository.deleteByDocumentId(documentId);
            kbDocumentParsedRepository.deleteById(documentId);
        });
        try {
            chunkVectorStore.deleteByDocumentId(documentId);
        } catch (Exception ex) {
            log.warn("Clear old ES vectors failed on parse start, documentId={}: {}", documentId, ex.getMessage());
        }
    }

    private void saveParsed(KbDocument document, String text) {
        transactionTemplate.executeWithoutResult(status -> {
            KbDocumentParsed parsed = kbDocumentParsedRepository.findById(document.getId())
                    .orElseGet(KbDocumentParsed::new);
            parsed.setDocumentId(document.getId());
            parsed.setContent(text);
            kbDocumentParsedRepository.save(parsed);

            KbDocument latest = kbDocumentRepository.findById(document.getId()).orElse(document);
            latest.setParsedCharCount(text.length());
            kbDocumentRepository.save(latest);
        });
    }

    private void markChunking(Long documentId) {
        transactionTemplate.executeWithoutResult(status -> {
            kbDocumentRepository.findById(documentId).ifPresent(document -> {
                document.setStatus(DocumentStatus.CHUNKING);
                document.setErrorMessage(null);
                kbDocumentRepository.save(document);
            });
        });
    }

    private void saveChunksAndWaitingEmbedding(Long documentId, Long kbId, List<String> pieces) {
        transactionTemplate.executeWithoutResult(status -> {
            kbDocumentChunkRepository.deleteByDocumentId(documentId);
            List<KbDocumentChunk> entities = new ArrayList<>();
            for (int i = 0; i < pieces.size(); i++) {
                String content = pieces.get(i);
                KbDocumentChunk chunk = new KbDocumentChunk();
                chunk.setDocumentId(documentId);
                chunk.setKbId(kbId);
                chunk.setChunkIndex(i);
                chunk.setContent(content);
                chunk.setCharCount(content.length());
                entities.add(chunk);
            }
            if (!entities.isEmpty()) {
                kbDocumentChunkRepository.saveAll(entities);
            }
            kbDocumentRepository.findById(documentId).ifPresent(document -> {
                document.setChunkCount(pieces.size());
                document.setStatus(DocumentStatus.WAITING_EMBEDDING);
                document.setErrorMessage(null);
                kbDocumentRepository.save(document);
            });
        });
    }

    private void markEmbedding(Long documentId) {
        transactionTemplate.executeWithoutResult(status -> {
            kbDocumentRepository.findById(documentId).ifPresent(document -> {
                document.setStatus(DocumentStatus.EMBEDDING);
                document.setErrorMessage(null);
                kbDocumentRepository.save(document);
            });
        });
    }

    private void markReady(Long documentId) {
        transactionTemplate.executeWithoutResult(status -> {
            kbDocumentRepository.findById(documentId).ifPresent(document -> {
                document.setStatus(DocumentStatus.READY);
                document.setErrorMessage(null);
                kbDocumentRepository.save(document);
            });
        });
    }

    @Transactional
    public void markFailed(Long documentId, String errorMessage) {
        if (documentId == null) {
            return;
        }
        kbDocumentRepository.findById(documentId).ifPresent(document -> {
            document.setStatus(DocumentStatus.FAILED);
            String msg = StringUtils.hasText(errorMessage) ? errorMessage : "入库失败";
            if (msg.length() > 500) {
                msg = msg.substring(0, 500);
            }
            document.setErrorMessage(msg);
            kbDocumentRepository.save(document);
            log.error("Document ingest failed: documentId={}, reason={}", documentId, msg);
        });
    }
}
