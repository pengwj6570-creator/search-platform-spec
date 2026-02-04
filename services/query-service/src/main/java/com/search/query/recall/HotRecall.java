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
 * Hot/popular items recall based on metrics like sales, views, or engagement
 */
@Component
public class HotRecall {

    private static final Logger log = LoggerFactory.getLogger(HotRecall.class);

    private final OpenSearchClient client;

    public HotRecall(OpenSearchClient client) {
        this.client = client;
    }

    /**
     * Recall hot/popular documents
     *
     * @param index the index name
     * @param sortField the field to sort by (e.g., sales, views, created_at)
     * @param topK number of results to return
     * @return list of recall results
     */
    public List<RecallResult> recall(String index, String sortField, int topK) {
        try {
            SearchResponse<Map> response = client.search(s -> s
                    .index(index)
                    .size(topK)
                    .sort(sort -> sort
                            .field(f -> f
                                    .field(sortField)
                                    .order(SortOrder.Desc)
                            )
                    ),
                    Map.class
            );

            return response.hits().hits().stream()
                    .map(hit -> new RecallResult(hit.id(), hit.score() != null ? hit.score() : 1.0f, "hot"))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Hot recall failed: index={}, sortField={}", index, sortField, e);
            return List.of();
        }
    }

    /**
     * Recall hot documents with category filter
     *
     * @param index the index name
     * @param sortField the field to sort by
     * @param categoryField the category field name
     * @param category the category value
     * @param topK number of results to return
     * @return list of recall results
     */
    public List<RecallResult> recallByCategory(String index, String sortField,
                                                 String categoryField, String category, int topK) {
        try {
            SearchResponse<Map> response = client.search(s -> s
                    .index(index)
                    .size(topK)
                    .query(q -> q
                            .term(t -> t
                                    .field(categoryField)
                                    .value(category)
                            )
                    )
                    .sort(sort -> sort
                            .field(f -> f
                                    .field(sortField)
                                    .order(SortOrder.Desc)
                            )
                    ),
                    Map.class
            );

            return response.hits().hits().stream()
                    .map(hit -> new RecallResult(hit.id(), hit.score() != null ? hit.score() : 1.0f, "hot"))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Hot recall by category failed: index={}, category={}", index, category, e);
            return List.of();
        }
    }

    /**
     * Recall recently created documents (trending)
     *
     * @param index the index name
     * @param dateField the date field name
     * @param topK number of results to return
     * @return list of recall results
     */
    public List<RecallResult> recallTrending(String index, String dateField, int topK) {
        return recall(index, dateField, topK);
    }
}
