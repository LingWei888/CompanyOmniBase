package cn.exitcode.richpeasants.ingest.mq;

import cn.exitcode.richpeasants.common.entity.KbDocument;

import java.io.Serializable;

/**
 * 文档异步入库消息（Day4）。
 */
public class DocumentIngestMessage implements Serializable {

    private Long documentId;
    private Long kbId;
    private String objectKey;
    private String originalFilename;

    public DocumentIngestMessage() {
    }

    public DocumentIngestMessage(Long documentId, Long kbId, String objectKey, String originalFilename) {
        this.documentId = documentId;
        this.kbId = kbId;
        this.objectKey = objectKey;
        this.originalFilename = originalFilename;
    }

    public static DocumentIngestMessage from(KbDocument document) {
        return new DocumentIngestMessage(
                document.getId(),
                document.getKbId(),
                document.getObjectKey(),
                document.getOriginalFilename()
        );
    }

    public Long getDocumentId() {
        return documentId;
    }

    public void setDocumentId(Long documentId) {
        this.documentId = documentId;
    }

    public Long getKbId() {
        return kbId;
    }

    public void setKbId(Long kbId) {
        this.kbId = kbId;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public void setObjectKey(String objectKey) {
        this.objectKey = objectKey;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }
}
