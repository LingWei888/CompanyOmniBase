package cn.exitcode.richpeasants.admin.service;

import cn.exitcode.richpeasants.admin.dto.ChunkDefaultsResponse;
import cn.exitcode.richpeasants.admin.dto.DocumentChunkDetailResponse;
import cn.exitcode.richpeasants.admin.dto.DocumentChunkItemResponse;
import cn.exitcode.richpeasants.admin.dto.DocumentParsedTextResponse;
import cn.exitcode.richpeasants.admin.dto.DocumentUpdateRequest;
import cn.exitcode.richpeasants.common.entity.KbDocument;
import cn.exitcode.richpeasants.common.entity.KbDocumentChunk;
import cn.exitcode.richpeasants.common.entity.KbDocumentParsed;
import cn.exitcode.richpeasants.common.entity.KnowledgeBase;
import cn.exitcode.richpeasants.common.entity.SysConfig;
import cn.exitcode.richpeasants.common.enums.DocumentStatus;
import cn.exitcode.richpeasants.common.exception.BusinessException;
import cn.exitcode.richpeasants.common.repository.KbDocumentChunkRepository;
import cn.exitcode.richpeasants.common.repository.KbDocumentParsedRepository;
import cn.exitcode.richpeasants.common.repository.KbDocumentRepository;
import cn.exitcode.richpeasants.common.repository.KnowledgeBaseRepository;
import cn.exitcode.richpeasants.common.repository.SysConfigRepository;
import cn.exitcode.richpeasants.common.result.PageResult;
import cn.exitcode.richpeasants.common.result.ResultCode;
import cn.exitcode.richpeasants.common.storage.MinioStorageService;
import cn.exitcode.richpeasants.ingest.es.ChunkVectorStore;
import cn.exitcode.richpeasants.ingest.mq.DocumentIngestPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;
import java.util.Set;

@Service
public class DocumentAdminService {

    public static final String KEY_CHUNK_SIZE = "ingest_chunk_size";
    public static final String KEY_CHUNK_OVERLAP = "ingest_chunk_overlap";
    public static final int DEFAULT_CHUNK_SIZE = 800;
    public static final int DEFAULT_CHUNK_OVERLAP = 100;

    private static final Set<String> ALLOWED_EXT = Set.of("pdf", "doc", "docx", "md", "txt", "markdown");

    private final KbDocumentRepository kbDocumentRepository;
    private final KbDocumentParsedRepository kbDocumentParsedRepository;
    private final KbDocumentChunkRepository kbDocumentChunkRepository;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final SysConfigRepository sysConfigRepository;
    private final MinioStorageService minioStorageService;
    private final DocumentIngestPublisher documentIngestPublisher;
    private final ChunkVectorStore chunkVectorStore;

    public DocumentAdminService(KbDocumentRepository kbDocumentRepository,
                                KbDocumentParsedRepository kbDocumentParsedRepository,
                                KbDocumentChunkRepository kbDocumentChunkRepository,
                                KnowledgeBaseRepository knowledgeBaseRepository,
                                SysConfigRepository sysConfigRepository,
                                MinioStorageService minioStorageService,
                                DocumentIngestPublisher documentIngestPublisher,
                                ChunkVectorStore chunkVectorStore) {
        this.kbDocumentRepository = kbDocumentRepository;
        this.kbDocumentParsedRepository = kbDocumentParsedRepository;
        this.kbDocumentChunkRepository = kbDocumentChunkRepository;
        this.knowledgeBaseRepository = knowledgeBaseRepository;
        this.sysConfigRepository = sysConfigRepository;
        this.minioStorageService = minioStorageService;
        this.documentIngestPublisher = documentIngestPublisher;
        this.chunkVectorStore = chunkVectorStore;
    }

    public PageResult<KbDocument> page(Long kbId, DocumentStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), normalizeSize(size));
        Page<KbDocument> data;
        if (kbId != null && status != null) {
            data = kbDocumentRepository.findByKbIdAndStatusOrderByIdDesc(kbId, status, pageable);
        } else if (kbId != null) {
            data = kbDocumentRepository.findByKbIdOrderByIdDesc(kbId, pageable);
        } else if (status != null) {
            data = kbDocumentRepository.findByStatusOrderByIdDesc(status, pageable);
        } else {
            data = kbDocumentRepository.findAllByOrderByIdDesc(pageable);
        }
        return PageResult.from(data);
    }

    public KbDocument get(Long id) {
        return kbDocumentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "文档不存在"));
    }

    public ChunkDefaultsResponse chunkDefaults(Long kbId) {
        int[] system = resolveSystemDefaults();
        ChunkDefaultsResponse response = new ChunkDefaultsResponse();
        response.setSystemChunkSize(system[0]);
        response.setSystemChunkOverlap(system[1]);

        Integer kbSize = null;
        Integer kbOverlap = null;
        if (kbId != null) {
            KnowledgeBase kb = knowledgeBaseRepository.findById(kbId).orElse(null);
            if (kb != null) {
                kbSize = kb.getDefaultChunkSize();
                kbOverlap = kb.getDefaultChunkOverlap();
            }
        }
        response.setKbChunkSize(kbSize);
        response.setKbChunkOverlap(kbOverlap);

        int[] effective = resolveChunkParams(kbId, null, null);
        response.setChunkSize(effective[0]);
        response.setChunkOverlap(effective[1]);
        return response;
    }

    public DocumentParsedTextResponse getParsedText(Long id) {
        KbDocument document = get(id);
        if (document.getParsedCharCount() == null || document.getParsedCharCount() <= 0) {
            throw new BusinessException(ResultCode.CONFLICT, "文档尚未完成文字解析，暂无可查看正文");
        }
        KbDocumentParsed parsed = kbDocumentParsedRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCode.CONFLICT, "文档尚未完成文字解析，暂无可查看正文"));
        return new DocumentParsedTextResponse(
                document.getId(),
                document.getTitle(),
                parsed.getContent(),
                document.getParsedCharCount()
        );
    }

    public PageResult<DocumentChunkItemResponse> pageChunks(Long documentId, int page, int size) {
        KbDocument document = get(documentId);
        if (document.getChunkCount() == null || document.getChunkCount() <= 0) {
            throw new BusinessException(ResultCode.CONFLICT, "文档尚未完成切分，暂无片段可查看");
        }
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), normalizeSize(size));
        Page<DocumentChunkItemResponse> data = kbDocumentChunkRepository
                .findByDocumentIdOrderByChunkIndexAsc(documentId, pageable)
                .map(this::toChunkItem);
        return PageResult.from(data);
    }

    public DocumentChunkDetailResponse getChunk(Long documentId, Long chunkId) {
        KbDocument document = get(documentId);
        KbDocumentChunk chunk = kbDocumentChunkRepository.findByIdAndDocumentId(chunkId, documentId)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "切分片段不存在"));
        DocumentChunkDetailResponse response = new DocumentChunkDetailResponse();
        response.setId(chunk.getId());
        response.setDocumentId(document.getId());
        response.setDocumentTitle(document.getTitle());
        response.setChunkIndex(chunk.getChunkIndex());
        response.setCharCount(chunk.getCharCount());
        response.setContent(chunk.getContent());
        return response;
    }

    private DocumentChunkItemResponse toChunkItem(KbDocumentChunk chunk) {
        DocumentChunkItemResponse item = new DocumentChunkItemResponse();
        item.setId(chunk.getId());
        item.setDocumentId(chunk.getDocumentId());
        item.setChunkIndex(chunk.getChunkIndex());
        item.setCharCount(chunk.getCharCount());
        item.setPreview(previewText(chunk.getContent(), 120));
        return item;
    }

    private String previewText(String content, int maxLen) {
        if (!StringUtils.hasText(content)) {
            return "";
        }
        String value = content.replaceAll("\\s+", " ").trim();
        if (value.length() <= maxLen) {
            return value;
        }
        return value.substring(0, maxLen) + "…";
    }

    @Transactional
    public KbDocument upload(Long kbId, String title, MultipartFile file, Integer chunkSize, Integer chunkOverlap) {
        if (!knowledgeBaseRepository.existsById(kbId)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "知识库不存在");
        }
        validateFile(file);
        int[] chunk = resolveChunkParams(kbId, chunkSize, chunkOverlap);
        String original = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        String objectKey = minioStorageService.upload(kbId, file);

        KbDocument document = new KbDocument();
        document.setKbId(kbId);
        document.setTitle(StringUtils.hasText(title) ? title.trim() : stripExtension(original));
        document.setOriginalFilename(original);
        document.setObjectKey(objectKey);
        document.setContentType(file.getContentType());
        document.setFileSize(file.getSize());
        document.setChunkSize(chunk[0]);
        document.setChunkOverlap(chunk[1]);
        document.setParsedCharCount(0);
        document.setChunkCount(0);
        document.setStatus(DocumentStatus.PENDING);
        KbDocument saved = kbDocumentRepository.save(document);
        documentIngestPublisher.publishAfterCommit(saved);
        return saved;
    }

    /**
     * 重新投递入库队列（全流程：解析 + 切分）。
     */
    @Transactional
    public KbDocument requeue(Long id) {
        KbDocument document = get(id);
        document.setStatus(DocumentStatus.PENDING);
        document.setErrorMessage(null);
        KbDocument saved = kbDocumentRepository.save(document);
        documentIngestPublisher.publishAfterCommit(saved);
        return saved;
    }

    /**
     * 手动触发 Embedding：文档须已切分完成（WAITING_EMBEDDING），或 FAILED 且已有片段。
     */
    @Transactional
    public KbDocument startEmbedding(Long id) {
        KbDocument document = get(id);
        if (document.getStatus() == DocumentStatus.EMBEDDING) {
            throw new BusinessException(ResultCode.CONFLICT, "文档正在向量化中");
        }
        if (document.getStatus() == DocumentStatus.READY) {
            throw new BusinessException(ResultCode.CONFLICT, "文档已完成向量化");
        }
        if (document.getStatus() == DocumentStatus.PENDING
                || document.getStatus() == DocumentStatus.PARSING
                || document.getStatus() == DocumentStatus.CHUNKING) {
            throw new BusinessException(ResultCode.CONFLICT, "文档尚未完成切分，请等待解析切分完成");
        }
        int chunks = document.getChunkCount() == null ? 0 : document.getChunkCount();
        if (chunks <= 0 && document.getStatus() != DocumentStatus.WAITING_EMBEDDING) {
            throw new BusinessException(ResultCode.CONFLICT, "文档没有可向量化的片段，请重新入库");
        }
        document.setStatus(DocumentStatus.WAITING_EMBEDDING);
        document.setErrorMessage(null);
        KbDocument saved = kbDocumentRepository.save(document);
        documentIngestPublisher.publishAfterCommit(saved);
        return saved;
    }

    /**
     * 替换上传：保留同一文档 ID，换新文件后重新入队。
     */
    @Transactional
    public KbDocument replace(Long id, String title, MultipartFile file, Integer chunkSize, Integer chunkOverlap) {
        KbDocument document = get(id);
        validateFile(file);
        // 未传参数时按 文档未覆盖 → 知识库默认 → 系统默认 解析（不沿用旧文档值）
        int[] chunk = resolveChunkParams(document.getKbId(), chunkSize, chunkOverlap);

        String oldObjectKey = document.getObjectKey();
        String original = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        String newObjectKey = minioStorageService.upload(document.getKbId(), file);

        if (StringUtils.hasText(title)) {
            document.setTitle(title.trim());
        }
        document.setOriginalFilename(original);
        document.setObjectKey(newObjectKey);
        document.setContentType(file.getContentType());
        document.setFileSize(file.getSize());
        document.setChunkSize(chunk[0]);
        document.setChunkOverlap(chunk[1]);
        document.setParsedCharCount(0);
        document.setChunkCount(0);
        document.setStatus(DocumentStatus.PENDING);
        document.setErrorMessage(null);
        KbDocument saved = kbDocumentRepository.save(document);
        documentIngestPublisher.publishAfterCommit(saved);

        if (StringUtils.hasText(oldObjectKey) && !oldObjectKey.equals(newObjectKey)) {
            try {
                minioStorageService.delete(oldObjectKey);
            } catch (BusinessException ignored) {
                // 新文件已落库并入队，旧对象残留不阻断替换
            }
        }
        return saved;
    }

    @Transactional
    public KbDocument update(Long id, DocumentUpdateRequest request) {
        KbDocument document = get(id);
        document.setTitle(request.getTitle().trim());

        Integer prevSize = document.getChunkSize();
        Integer prevOverlap = document.getChunkOverlap();
        if (request.getChunkSize() != null || request.getChunkOverlap() != null) {
            Integer sizeInput = request.getChunkSize() != null ? request.getChunkSize() : prevSize;
            Integer overlapInput = request.getChunkOverlap() != null ? request.getChunkOverlap() : prevOverlap;
            int[] chunk = resolveChunkParams(document.getKbId(), sizeInput, overlapInput);
            document.setChunkSize(chunk[0]);
            document.setChunkOverlap(chunk[1]);
        }
        validateChunkPair(document.getChunkSize(), document.getChunkOverlap());

        boolean chunkChanged = !document.getChunkSize().equals(prevSize)
                || !document.getChunkOverlap().equals(prevOverlap);
        KbDocument saved = kbDocumentRepository.save(document);
        if (chunkChanged) {
            saved.setStatus(DocumentStatus.PENDING);
            saved.setErrorMessage(null);
            saved = kbDocumentRepository.save(saved);
            documentIngestPublisher.publishAfterCommit(saved);
        }
        return saved;
    }

    @Transactional
    public void delete(Long id) {
        KbDocument document = get(id);
        String objectKey = document.getObjectKey();
        try {
            chunkVectorStore.deleteByDocumentId(id);
        } catch (Exception ignored) {
            // ES 清理失败不阻断元数据删除
        }
        minioStorageService.delete(objectKey);
        kbDocumentRepository.delete(document);
    }

    private int[] resolveSystemDefaults() {
        int size = parseIntConfig(KEY_CHUNK_SIZE, DEFAULT_CHUNK_SIZE);
        int overlap = parseIntConfig(KEY_CHUNK_OVERLAP, DEFAULT_CHUNK_OVERLAP);
        if (overlap >= size) {
            overlap = Math.max(0, size / 10);
        }
        return new int[]{size, overlap};
    }

    /**
     * 优先级：文档入参 → 知识库默认 → 系统默认。
     */
    private int[] resolveChunkParams(Long kbId, Integer chunkSize, Integer chunkOverlap) {
        int[] system = resolveSystemDefaults();
        Integer kbSize = null;
        Integer kbOverlap = null;
        if (kbId != null) {
            KnowledgeBase kb = knowledgeBaseRepository.findById(kbId).orElse(null);
            if (kb != null) {
                kbSize = kb.getDefaultChunkSize();
                kbOverlap = kb.getDefaultChunkOverlap();
            }
        }
        int size = chunkSize != null ? chunkSize : (kbSize != null ? kbSize : system[0]);
        int overlap = chunkOverlap != null ? chunkOverlap : (kbOverlap != null ? kbOverlap : system[1]);
        validateChunkPair(size, overlap);
        return new int[]{size, overlap};
    }

    private void validateChunkPair(Integer chunkSize, Integer chunkOverlap) {
        if (chunkSize == null || chunkSize < 100 || chunkSize > 8000) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "切分长度需在 100-8000 之间");
        }
        if (chunkOverlap == null || chunkOverlap < 0 || chunkOverlap > 4000) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "重叠长度需在 0-4000 之间");
        }
        if (chunkOverlap >= chunkSize) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "重叠长度必须小于切分长度");
        }
    }

    private int parseIntConfig(String key, int fallback) {
        return sysConfigRepository.findByConfigKey(key)
                .map(SysConfig::getConfigValue)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .map(value -> {
                    try {
                        return Integer.parseInt(value);
                    } catch (NumberFormatException ex) {
                        return fallback;
                    }
                })
                .orElse(fallback);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "请选择要上传的文件");
        }
        String filename = file.getOriginalFilename();
        if (!StringUtils.hasText(filename) || !filename.contains(".")) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "不支持的文件类型");
        }
        String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXT.contains(ext)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "仅支持 pdf/doc/docx/md/txt");
        }
    }

    private String stripExtension(String filename) {
        int idx = filename.lastIndexOf('.');
        return idx > 0 ? filename.substring(0, idx) : filename;
    }

    private int normalizeSize(int size) {
        if (size <= 0) {
            return 10;
        }
        return Math.min(size, 100);
    }
}
