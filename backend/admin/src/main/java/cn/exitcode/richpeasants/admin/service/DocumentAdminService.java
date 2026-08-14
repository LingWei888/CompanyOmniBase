package cn.exitcode.richpeasants.admin.service;

import cn.exitcode.richpeasants.admin.dto.DocumentUpdateRequest;
import cn.exitcode.richpeasants.common.entity.KbDocument;
import cn.exitcode.richpeasants.common.enums.DocumentStatus;
import cn.exitcode.richpeasants.common.exception.BusinessException;
import cn.exitcode.richpeasants.common.repository.KbDocumentRepository;
import cn.exitcode.richpeasants.common.repository.KnowledgeBaseRepository;
import cn.exitcode.richpeasants.common.result.PageResult;
import cn.exitcode.richpeasants.common.result.ResultCode;
import cn.exitcode.richpeasants.common.storage.MinioStorageService;
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

    private static final Set<String> ALLOWED_EXT = Set.of("pdf", "doc", "docx", "md", "txt", "markdown");

    private final KbDocumentRepository kbDocumentRepository;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final MinioStorageService minioStorageService;

    public DocumentAdminService(KbDocumentRepository kbDocumentRepository,
                                KnowledgeBaseRepository knowledgeBaseRepository,
                                MinioStorageService minioStorageService) {
        this.kbDocumentRepository = kbDocumentRepository;
        this.knowledgeBaseRepository = knowledgeBaseRepository;
        this.minioStorageService = minioStorageService;
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

    @Transactional
    public KbDocument upload(Long kbId, String title, MultipartFile file) {
        if (!knowledgeBaseRepository.existsById(kbId)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "知识库不存在");
        }
        validateFile(file);
        String original = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        String objectKey = minioStorageService.upload(kbId, file);

        KbDocument document = new KbDocument();
        document.setKbId(kbId);
        document.setTitle(StringUtils.hasText(title) ? title.trim() : stripExtension(original));
        document.setOriginalFilename(original);
        document.setObjectKey(objectKey);
        document.setContentType(file.getContentType());
        document.setFileSize(file.getSize());
        document.setStatus(DocumentStatus.PENDING);
        return kbDocumentRepository.save(document);
    }

    @Transactional
    public KbDocument update(Long id, DocumentUpdateRequest request) {
        KbDocument document = get(id);
        document.setTitle(request.getTitle().trim());
        return kbDocumentRepository.save(document);
    }

    @Transactional
    public void delete(Long id) {
        KbDocument document = get(id);
        String objectKey = document.getObjectKey();
        minioStorageService.delete(objectKey);
        kbDocumentRepository.delete(document);
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
