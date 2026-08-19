package cn.exitcode.richpeasants.ingest.es;

import cn.exitcode.richpeasants.common.exception.BusinessException;
import cn.exitcode.richpeasants.common.result.ResultCode;
import cn.exitcode.richpeasants.ingest.config.IngestAppProperties;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import co.elastic.clients.elasticsearch.indices.GetMappingResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ChunkVectorStore {

    private static final Logger log = LoggerFactory.getLogger(ChunkVectorStore.class);

    private final ElasticsearchClient elasticsearchClient;
    private final IngestAppProperties ingestAppProperties;
    private volatile Integer ensuredDims;

    public ChunkVectorStore(ElasticsearchClient elasticsearchClient,
                            IngestAppProperties ingestAppProperties) {
        this.elasticsearchClient = elasticsearchClient;
        this.ingestAppProperties = ingestAppProperties;
    }

    public String indexName() {
        return ingestAppProperties.getElasticsearch().getChunkIndex();
    }

    public synchronized void ensureIndex(int dims) {
        if (dims <= 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "向量维度无效: " + dims);
        }
        if (ensuredDims != null && ensuredDims == dims) {
            return;
        }
        String index = indexName();
        try {
            boolean exists = elasticsearchClient.indices()
                    .exists(ExistsRequest.of(e -> e.index(index)))
                    .value();
            if (exists) {
                Integer existingDims = readEmbeddingDims(index);
                if (existingDims != null && existingDims != dims) {
                    log.warn("ES index {} dims mismatch: mapping={}, model={}, recreating index",
                            index, existingDims, dims);
                    elasticsearchClient.indices().delete(d -> d.index(index));
                    exists = false;
                } else if (existingDims != null) {
                    log.info("ES index {} already exists with dims={}", index, existingDims);
                    ensuredDims = dims;
                    return;
                }
            }
            if (!exists) {
                createIndex(index, dims);
            }
            ensuredDims = dims;
        } catch (BusinessException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "创建/检查 ES 索引失败: " + ex.getMessage());
        }
    }

    private void createIndex(String index, int dims) throws IOException {
        String body = """
                {
                  "mappings": {
                    "properties": {
                      "documentId": { "type": "long" },
                      "kbId": { "type": "long" },
                      "chunkId": { "type": "long" },
                      "chunkIndex": { "type": "integer" },
                      "content": { "type": "text" },
                      "modelId": { "type": "long" },
                      "embedding": {
                        "type": "dense_vector",
                        "dims": %d,
                        "index": true,
                        "similarity": "cosine"
                      }
                    }
                  }
                }
                """.formatted(dims);
        elasticsearchClient.indices().create(c -> c
                .index(index)
                .withJson(new StringReader(body))
        );
        log.info("Created ES index {} with dense_vector dims={}", index, dims);
    }

    private Integer readEmbeddingDims(String index) {
        try {
            GetMappingResponse mapping = elasticsearchClient.indices().getMapping(g -> g.index(index));
            if (mapping.result() == null || mapping.result().isEmpty()) {
                return null;
            }
            var indexMapping = mapping.result().values().iterator().next();
            if (indexMapping.mappings() == null || indexMapping.mappings().properties() == null) {
                return null;
            }
            Property embedding = indexMapping.mappings().properties().get("embedding");
            if (embedding == null || embedding.denseVector() == null) {
                return null;
            }
            Number dims = embedding.denseVector().dims();
            return dims == null ? null : dims.intValue();
        } catch (Exception ex) {
            log.warn("Failed to read ES mapping dims for {}: {}", index, ex.getMessage());
            return null;
        }
    }

    public void deleteByDocumentId(Long documentId) {
        if (documentId == null) {
            return;
        }
        String index = indexName();
        try {
            boolean exists = elasticsearchClient.indices()
                    .exists(ExistsRequest.of(e -> e.index(index)))
                    .value();
            if (!exists) {
                return;
            }
            elasticsearchClient.deleteByQuery(d -> d
                    .index(index)
                    .query(q -> q.term(t -> t.field("documentId").value(documentId)))
                    .refresh(true)
            );
        } catch (IOException ex) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "删除 ES 向量失败: " + ex.getMessage());
        }
    }

    /**
     * 按知识库列表（空=全部）与可选 Embedding 模型做 dense_vector kNN 检索。
     */
    public List<RetrievedChunk> knnSearch(List<Long> kbIds, Long embeddingModelId, float[] queryVector, int topK) {
        if (queryVector == null || queryVector.length == 0 || topK <= 0) {
            return List.of();
        }
        String index = indexName();
        try {
            boolean exists = elasticsearchClient.indices()
                    .exists(ExistsRequest.of(e -> e.index(index)))
                    .value();
            if (!exists) {
                return List.of();
            }
            List<Float> vector = toFloatList(queryVector);
            //取大概的前多少个
            //ANN（近似最近邻，Approximate Nearest Neighbor） 算法。
            int candidates = Math.max(topK * 10, 50);
            List<Long> filterKbIds = kbIds == null ? List.of() : kbIds.stream()
                    .filter(id -> id != null && id > 0)
                    .distinct()
                    .toList();

            SearchResponse<Map> response = elasticsearchClient.search(s -> {
                s.index(index)
                        .size(topK)
                        .source(src -> src.filter(f -> f.includes(
                                "documentId", "kbId", "chunkId", "chunkIndex", "content", "modelId"
                        )))
                        .knn(k -> {
                            k.field("embedding")
                                    .queryVector(vector)
                                    .k((long) topK)
                                    .numCandidates((long) candidates)
                                    .filter(f -> f.bool(b -> {
                                        if (embeddingModelId != null) {
                                            b.must(m -> m.term(t -> t.field("modelId").value(embeddingModelId)));
                                        }
                                        if (filterKbIds.size() == 1) {
                                            Long only = filterKbIds.get(0);
                                            b.must(m -> m.term(t -> t.field("kbId").value(only)));
                                        } else if (filterKbIds.size() > 1) {
                                            List<FieldValue> values = filterKbIds.stream()
                                                    .map(FieldValue::of)
                                                    .toList();
                                            b.must(m -> m.terms(t -> t.field("kbId").terms(tv -> tv.value(values))));
                                        }
                                        return b;
                                    }));
                            return k;
                        });
                return s;
            }, Map.class);

            List<RetrievedChunk> hits = new ArrayList<>();
            for (Hit<Map> hit : response.hits().hits()) {
                Map<String, Object> source = hit.source();
                if (source == null) {
                    continue;
                }
                hits.add(new RetrievedChunk(
                        asLong(source.get("chunkId")),
                        asLong(source.get("documentId")),
                        asLong(source.get("kbId")),
                        asInt(source.get("chunkIndex")),
                        asString(source.get("content")),
                        hit.score() == null ? 0d : hit.score()
                ));
            }
            return hits;
        } catch (IOException ex) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "ES kNN 检索失败: " + ex.getMessage());
        }
    }

    /** @deprecated 使用 {@link #knnSearch(List, Long, float[], int)} */
    public List<RetrievedChunk> knnSearch(Long kbId, Long embeddingModelId, float[] queryVector, int topK) {
        return knnSearch(kbId == null ? List.of() : List.of(kbId), embeddingModelId, queryVector, topK);
    }

    private static Long asLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static Integer asInt(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static String asString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    public void indexChunks(Long documentId,
                            Long kbId,
                            Long modelId,
                            List<ChunkVectorRecord> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        String index = indexName();
        List<BulkOperation> ops = new ArrayList<>(records.size());
        for (ChunkVectorRecord record : records) {
            Map<String, Object> source = new HashMap<>();
            source.put("documentId", documentId);
            source.put("kbId", kbId);
            source.put("chunkId", record.chunkId());
            source.put("chunkIndex", record.chunkIndex());
            source.put("content", record.content());
            source.put("modelId", modelId);
            source.put("embedding", toFloatList(record.embedding()));
            String id = documentId + "_" + record.chunkIndex();
            ops.add(BulkOperation.of(b -> b.index(i -> i
                    .index(index)
                    .id(id)
                    .document(source)
            )));
        }
        try {
            BulkResponse response = elasticsearchClient.bulk(BulkRequest.of(b -> b
                    .operations(ops)
                    .refresh(co.elastic.clients.elasticsearch._types.Refresh.True)
            ));
            if (response.errors()) {
                String first = response.items().stream()
                        .filter(item -> item.error() != null)
                        .findFirst()
                        .map(item -> item.error().reason())
                        .orElse("unknown");
                throw new BusinessException(ResultCode.INTERNAL_ERROR, "ES bulk 写入失败: " + first);
            }
        } catch (BusinessException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "写入 ES 向量失败: " + ex.getMessage());
        }
    }

    private List<Float> toFloatList(float[] embedding) {
        List<Float> list = new ArrayList<>(embedding.length);
        for (float v : embedding) {
            list.add(v);
        }
        return list;
    }

    public record ChunkVectorRecord(Long chunkId, Integer chunkIndex, String content, float[] embedding) {
    }

    public record RetrievedChunk(Long chunkId,
                                 Long documentId,
                                 Long kbId,
                                 Integer chunkIndex,
                                 String content,
                                 double score) {
    }
}
