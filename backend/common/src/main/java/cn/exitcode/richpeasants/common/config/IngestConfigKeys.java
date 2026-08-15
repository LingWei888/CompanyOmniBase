package cn.exitcode.richpeasants.common.config;

public final class IngestConfigKeys {

    public static final String CHUNK_SIZE = "ingest_chunk_size";
    public static final String CHUNK_OVERLAP = "ingest_chunk_overlap";

    public static final int DEFAULT_CHUNK_SIZE = 800;
    public static final int DEFAULT_CHUNK_OVERLAP = 100;
    public static final int MIN_CHUNK_SIZE = 100;
    public static final int MAX_CHUNK_SIZE = 10000;

    private IngestConfigKeys() {
    }
}
