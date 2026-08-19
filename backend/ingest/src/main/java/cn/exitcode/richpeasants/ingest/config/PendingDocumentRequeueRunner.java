package cn.exitcode.richpeasants.ingest.config;

import cn.exitcode.richpeasants.common.entity.KbDocument;
import cn.exitcode.richpeasants.common.enums.DocumentStatus;
import cn.exitcode.richpeasants.common.repository.KbDocumentRepository;
import cn.exitcode.richpeasants.ingest.mq.DocumentIngestPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 启动补偿：仅重新投递 PENDING（解析切分）；WAITING_EMBEDDING 需后台手动触发。
 */
@Component
public class PendingDocumentRequeueRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PendingDocumentRequeueRunner.class);

    private final KbDocumentRepository kbDocumentRepository;
    private final DocumentIngestPublisher documentIngestPublisher;

    public PendingDocumentRequeueRunner(KbDocumentRepository kbDocumentRepository,
                                        DocumentIngestPublisher documentIngestPublisher) {
        this.kbDocumentRepository = kbDocumentRepository;
        this.documentIngestPublisher = documentIngestPublisher;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<KbDocument> docs = kbDocumentRepository.findByStatusOrderByIdAsc(DocumentStatus.PENDING);
        if (docs.isEmpty()) {
            return;
        }
        log.info("Requeue {} PENDING document(s) on startup", docs.size());
        for (KbDocument document : docs) {
            try {
                documentIngestPublisher.publish(document);
            } catch (Exception ex) {
                log.error("Failed to requeue documentId={}: {}", document.getId(), ex.getMessage());
            }
        }
    }
}
