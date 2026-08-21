package cn.exitcode.richpeasants.rag.memory;

import cn.exitcode.richpeasants.common.exception.BusinessException;
import cn.exitcode.richpeasants.common.result.ResultCode;
import cn.exitcode.richpeasants.ingest.config.IngestAppProperties;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
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

/**
 * 用户长期记忆向量库（ES dense_vector），与知识库索引分离。
 */
@Service
public class MemoryVectorStore {

    private static final Logger log = LoggerFactory.getLogger(MemoryVectorStore.class);

    private final ElasticsearchClient elasticsearchClient;
    private final IngestAppProperties ingestAppProperties;
    private volatile Integer ensuredDims;

    public MemoryVectorStore(ElasticsearchClient elasticsearchClient,
                            IngestAppProperties ingestAppProperties) {
        this.elasticsearchClient = elasticsearchClient;
        this.ingestAppProperties = ingestAppProperties;
    }

    public String indexName() {
        return ingestAppProperties.getElasticsearch().getMemoryIndex();
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
                    log.warn("ES memory index {} dims mismatch: mapping={}, model={}, recreating",
                            index, existingDims, dims);
                    elasticsearchClient.indices().delete(d -> d.index(index));
                    exists = false;
                } else if (existingDims != null) {
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
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "创建/检查记忆索引失败: " + ex.getMessage());
        }
    }

    private void createIndex(String index, int dims) throws IOException {
        String body = """
                {
                  "mappings": {
                    "properties": {
                      "memoryId": { "type": "long" },
                      "userId": { "type": "long" },
                      "content": { "type": "text" },
                      "category": { "type": "keyword" },
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
        log.info("Created ES memory index {} with dense_vector dims={}", index, dims);
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
            return embedding.denseVector().dims();
        } catch (Exception ex) {
            log.warn("Read memory index dims failed: {}", ex.getMessage());
            return null;
        }
    }

    public void indexMemory(Long memoryId, Long userId, Long modelId, String content, String category, float[] embedding) {
        if (memoryId == null || userId == null || embedding == null || embedding.length == 0) {
            return;
        }
        ensureIndex(embedding.length);
        String index = indexName();
        Map<String, Object> source = new HashMap<>();
        source.put("memoryId", memoryId);
        source.put("userId", userId);
        source.put("content", content == null ? "" : content);
        source.put("category", category == null ? "" : category);
        source.put("modelId", modelId);
        source.put("embedding", toFloatList(embedding));
        try {
            BulkResponse response = elasticsearchClient.bulk(BulkRequest.of(b -> b
                    .operations(List.of(BulkOperation.of(op -> op.index(i -> i
                            .index(index)
                            .id(String.valueOf(memoryId))
                            .document(source)
                    ))))
                    .refresh(co.elastic.clients.elasticsearch._types.Refresh.True)
            ));
            if (response.errors()) {
                String first = response.items().stream()
                        .filter(item -> item.error() != null)
                        .findFirst()
                        .map(item -> item.error().reason())
                        .orElse("unknown");
                throw new BusinessException(ResultCode.INTERNAL_ERROR, "记忆向量写入失败: " + first);
            }
        } catch (BusinessException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "写入记忆向量失败: " + ex.getMessage());
        }
    }

    public void deleteMemory(Long memoryId) {
        if (memoryId == null) {
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
            elasticsearchClient.delete(d -> d.index(index).id(String.valueOf(memoryId))
                    .refresh(co.elastic.clients.elasticsearch._types.Refresh.True));
        } catch (IOException ex) {
            log.warn("Delete memory vector {} failed: {}", memoryId, ex.getMessage());
        }
    }

    public void deleteByUserId(Long userId) {
        if (userId == null) {
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
                    .query(q -> q.term(t -> t.field("userId").value(userId)))
                    .refresh(true)
            );
        } catch (IOException ex) {
            log.warn("Delete user memory vectors failed userId={}: {}", userId, ex.getMessage());
        }
    }

    public List<RetrievedMemory> knnSearch(Long userId, Long embeddingModelId, float[] queryVector, int topK) {
        if (userId == null || queryVector == null || queryVector.length == 0 || topK <= 0) {
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
            int candidates = Math.max(topK * 10, 50);
            SearchResponse<Map> response = elasticsearchClient.search(s -> {
                s.index(index)
                        .size(topK)
                        .source(src -> src.filter(f -> f.includes("memoryId", "userId", "content", "category", "modelId")))
                        .knn(k -> {
                            k.field("embedding")
                                    .queryVector(vector)
                                    .k((long) topK)
                                    .numCandidates((long) candidates)
                                    .filter(f -> f.bool(b -> {
                                        b.must(m -> m.term(t -> t.field("userId").value(userId)));
                                        if (embeddingModelId != null) {
                                            b.must(m -> m.term(t -> t.field("modelId").value(embeddingModelId)));
                                        }
                                        return b;
                                    }));
                            return k;
                        });
                return s;
            }, Map.class);

            List<RetrievedMemory> hits = new ArrayList<>();
            for (Hit<Map> hit : response.hits().hits()) {
                Map<String, Object> source = hit.source();
                if (source == null) {
                    continue;
                }
                hits.add(new RetrievedMemory(
                        asLong(source.get("memoryId")),
                        asString(source.get("content")),
                        asString(source.get("category")),
                        hit.score() == null ? 0d : hit.score()
                ));
            }
            return hits;
        } catch (IOException ex) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "记忆 kNN 检索失败: " + ex.getMessage());
        }
    }

    private List<Float> toFloatList(float[] embedding) {
        List<Float> list = new ArrayList<>(embedding.length);
        for (float v : embedding) {
            list.add(v);
        }
        return list;
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

    private static String asString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    public record RetrievedMemory(Long memoryId, String content, String category, double score) {
    }
}
