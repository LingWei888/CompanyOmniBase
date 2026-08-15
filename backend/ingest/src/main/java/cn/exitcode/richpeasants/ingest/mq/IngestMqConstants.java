package cn.exitcode.richpeasants.ingest.mq;

public final class IngestMqConstants {

    public static final String EXCHANGE = "kb.document.ingest.exchange";
    public static final String QUEUE = "kb.document.ingest.queue";
    public static final String ROUTING_KEY = "kb.document.ingest";
    public static final String DLX = "kb.document.ingest.dlx";
    public static final String DLQ = "kb.document.ingest.dlq";

    private IngestMqConstants() {
    }
}
