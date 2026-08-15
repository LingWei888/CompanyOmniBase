package cn.exitcode.richpeasants.ingest.mq;

import cn.exitcode.richpeasants.common.entity.KbDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class DocumentIngestPublisher {

    private static final Logger log = LoggerFactory.getLogger(DocumentIngestPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public DocumentIngestPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publish(KbDocument document) {
        DocumentIngestMessage message = DocumentIngestMessage.from(document);
        rabbitTemplate.convertAndSend(
                IngestMqConstants.EXCHANGE,
                IngestMqConstants.ROUTING_KEY,
                message
        );
        log.info("Published document ingest message: documentId={}, kbId={}",
                message.getDocumentId(), message.getKbId());
    }

    /**
     * 事务提交后再投递，避免入库回滚后仍有脏消息。
     */
    public void publishAfterCommit(KbDocument document) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publish(document);
                }
            });
        } else {
            publish(document);
        }
    }
}
