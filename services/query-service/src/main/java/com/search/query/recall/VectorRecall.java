package com.search.query.recall;

import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Vector-based recall using semantic search with embeddings
 */
@Component
public class VectorRecall {

    private static final Logger log = LoggerFactory.getLogger(VectorRecall.class);

    private final OpenSearchClient client;
    private final VectorEmbeddingService embeddingService;

    public VectorRecall(OpenSearchClient client, VectorEmbeddingService embeddingService) {
        this.client = client;
        this.embeddingService = embeddingService;
    }

    /**
     * Recall documents using vector similarity search
     *
     * @param index the index name
     * @param query the search query text
     * @param vectorField the name of the vector field
     * @param topK number of results to return
     * @return list of recall results
     */
    public List<RecallResult> recall(String index, String query, String vectorField, int topK) {
        try {
            // Generate query embedding
            float[] queryVector = embeddingService.embed(query);

            if (queryVector == null || queryVector.length == 0) {
                log.warn("Failed to generate embedding for query: {}", query);
                return List.of();
            }

            SearchResponse<Map> response = client.search(s -> s
                    .index(index)
                    .size(topK)
                    .query(q -> q
                            .knn(k -> k
                                    .field(vectorField)
                                    .queryVector(queryVector)
                                    .k(topK)
                                    .numCandidates(topK * 10)
                            )
                    ),
                    Map.class
            );

            return response.hits().hits().stream()
                    .map(hit -> new RecallResult(hit.id(),
                            hit.score() != null ? hit.score().floatValue() : 0.0f, "vector"))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Vector recall failed: index={}, query={}", index, query, e);
            return List.of();
        }
    }

    /**
     * Recall documents using vector search with pre-computed embedding
     *
     * @param index the index name
     * @param queryVector the pre-computed query embedding
     * @param vectorField the name of the vector field
     * @param topK number of results to return
     * @return list of recall results
     */
    public List<RecallResult> recallWithVector(String index, float[] queryVector,
                                                 String vectorField, int topK) {
        try {
            if (queryVector == null || queryVector.length == 0) {
                log.warn("Empty query vector provided");
                return List.of();
            }

            SearchResponse<Map> response = client.search(s -> s
                    .index(index)
                    .size(topK)
                    .query(q -> q
                            .knn(k -> k
                                    .field(vectorField)
                                    .queryVector(queryVector)
                                    .k(topK)
                                    .numCandidates(topK * 10)
                            )
                    ),
                    Map.class
            );

            return response.hits().hits().stream()
                    .map(hit -> new RecallResult(hit.id(),
                            hit.score() != null ? hit.score().floatValue() : 0.0f, "vector"))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Vector recall with pre-computed vector failed: index={}", index, e);
            return List.of();
        }
    }
}
