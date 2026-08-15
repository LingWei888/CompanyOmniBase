package cn.exitcode.richpeasants.ingest.mq;

import cn.exitcode.richpeasants.ingest.service.DocumentIngestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class DocumentIngestListener {

    private static final Logger log = LoggerFactory.getLogger(DocumentIngestListener.class);

    private final DocumentIngestService documentIngestService;

    public DocumentIngestListener(DocumentIngestService documentIngestService) {
        this.documentIngestService = documentIngestService;
    }

    @RabbitListener(queues = IngestMqConstants.QUEUE)
    public void onMessage(DocumentIngestMessage message) {
        try {
            log.debug("Received ingest message: {}", message == null ? null : message.getDocumentId());
            documentIngestService.handleIngestMessage(message);
        } catch (Exception ex) {
            Long documentId = message == null ? null : message.getDocumentId();
            log.error("Ingest handler error, mark FAILED. documentId={}", documentId, ex);
            documentIngestService.markFailed(documentId, ex.getMessage());
        }
    }
}
