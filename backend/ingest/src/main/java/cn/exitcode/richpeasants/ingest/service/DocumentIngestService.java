package cn.exitcode.richpeasants.ingest.service;

import cn.exitcode.richpeasants.common.entity.KbDocument;
import cn.exitcode.richpeasants.common.enums.DocumentStatus;
import cn.exitcode.richpeasants.common.repository.KbDocumentRepository;
import cn.exitcode.richpeasants.ingest.mq.DocumentIngestMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Service
public class DocumentIngestService {

    private static final Logger log = LoggerFactory.getLogger(DocumentIngestService.class);

    private final KbDocumentRepository kbDocumentRepository;

    public DocumentIngestService(KbDocumentRepository kbDocumentRepository) {
        this.kbDocumentRepository = kbDocumentRepository;
    }

    /**
     * Day4：消费入库消息，将文档标记为 PROCESSING。
     * 真正的解析切分 / ES 写入留给 Day5~6。
     */
    @Transactional
    public void handleIngestMessage(DocumentIngestMessage message) {
        if (message == null || message.getDocumentId() == null) {
            log.warn("Ignore empty ingest message");
            return;
        }
        Optional<KbDocument> optional = kbDocumentRepository.findById(message.getDocumentId());
        if (optional.isEmpty()) {
            log.warn("Document not found for ingest: {}", message.getDocumentId());
            return;
        }
        KbDocument document = optional.get();
        if (document.getStatus() == DocumentStatus.READY) {
            log.info("Skip already READY document: {}", document.getId());
            return;
        }
        if (document.getStatus() == DocumentStatus.PROCESSING) {
            log.info("Document already PROCESSING, keep waiting Day5 parse: {}", document.getId());
            return;
        }

        document.setStatus(DocumentStatus.PROCESSING);
        document.setErrorMessage(null);
        kbDocumentRepository.save(document);
        log.info("Document claimed for ingest (PROCESSING). documentId={}, objectKey={}, filename={}. Parsing deferred to Day5.",
                document.getId(), document.getObjectKey(), document.getOriginalFilename());
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
