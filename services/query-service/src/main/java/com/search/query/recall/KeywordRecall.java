package com.search.query.recall;

import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.SortOrder;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Keyword-based recall using OpenSearch full-text search
 */
@Component
public class KeywordRecall {

    private static final Logger log = LoggerFactory.getLogger(KeywordRecall.class);

    private final OpenSearchClient client;

    public KeywordRecall(OpenSearchClient client) {
        this.client = client;
    }

    /**
     * Recall documents using keyword search
     *
     * @param index the index name
     * @param query the search query
     * @param topK number of results to return
     * @return list of recall results
     */
    public List<RecallResult> recall(String index, String query, int topK) {
        try {
            SearchResponse<Map> response = client.search(s -> s
                    .index(index)
                    .size(topK)
                    .query(q -> q
                            .simpleString(sq -> sq
                                    .fields("title^2", "description", "content")
                                    .query(query)
                            )
                    ),
                    Map.class
            );

            return response.hits().hits().stream()
                    .map(hit -> new RecallResult(hit.id(), hit.score() != null ? hit.score() : 0.0f, "keyword"))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Keyword recall failed: index={}, query={}", index, query, e);
            return List.of();
        }
    }

    /**
     * Recall documents with filters
     *
     * @param index the index name
     * @param query the search query
     * @param filters filter criteria
     * @param topK number of results to return
     * @return list of recall results
     */
    public List<RecallResult> recallWithFilters(String index, String query,
                                                  Map<String, Object> filters, int topK) {
        try {
            SearchResponse<Map> response = client.search(s -> {
                SearchRequest.Builder builder = s
                        .index(index)
                        .size(topK)
                        .query(q -> q
                                .bool(b -> {
                                    if (query != null && !query.isEmpty()) {
                                        b.must(m -> m.simpleString(sq -> sq
                                                .fields("title^2", "description", "content")
                                                .query(query)
                                        ));
                                    }
                                    // Add filters
                                    if (filters != null) {
                                        for (Map.Entry<String, Object> filter : filters.entrySet()) {
                                            b.filter(f -> f
                                                    .term(t -> t
                                                            .field(filter.getKey())
                                                            .value(filter.getValue().toString())
                                                    )
                                            );
                                        }
                                    }
                                    return b;
                                })
                        );
                return builder;
            }, Map.class);

            return response.hits().hits().stream()
                    .map(hit -> new RecallResult(hit.id(), hit.score() != null ? hit.score() : 0.0f, "keyword"))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Keyword recall with filters failed: index={}, query={}", index, query, e);
            return List.of();
        }
    }
}
