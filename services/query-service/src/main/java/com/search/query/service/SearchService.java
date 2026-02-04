package com.search.query.service;

import com.search.query.model.SearchRequest;
import com.search.query.model.SearchResponse;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.SearchRequest as OSSearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse as OSSearchResponse;
import org.opensearch.client.opensearch.core.search.Hit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Search service for executing queries against OpenSearch
 */
@Service
public class SearchService {

    private static final Logger log = LoggerFactory.getLogger(SearchService.class);

    private final OpenSearchClient client;
    private final String indexPrefix;

    public SearchService(OpenSearchClient client) {
        this.client = client;
        this.indexPrefix = "search";
    }

    /**
     * Execute a search query
     *
     * @param request the search request
     * @return search response with results
     */
    public SearchResponse search(SearchRequest request) {
        long startTime = System.currentTimeMillis();

        try {
            String indexName = getIndexName(request.getAppKey());

            // Build the OpenSearch query
            OSSearchRequest.Builder searchBuilder = new OSSearchRequest.Builder()
                    .index(indexName)
                    .from((request.getPage() - 1) * request.getPageSize())
                    .size(request.getPageSize());

            // Build query based on request
            searchBuilder.query(q -> buildQuery(request, q));

            // Add sorting if specified
            if (request.getSort() != null && request.getSort().getField() != null) {
                String order = request.getSort().getOrder();
                boolean desc = "desc".equalsIgnoreCase(order);
                searchBuilder.sort(s -> s
                        .field(f -> f
                                .field(request.getSort().getField())
                                .order(desc ? org.opensearch.client.opensearch._types.SortOrder.Desc :
                                              org.opensearch.client.opensearch._types.SortOrder.Asc)
                        )
                );
            }

            OSSearchResponse<Map> response = client.search(
                    searchBuilder.build(),
                    Map.class
            );

            // Build response
            SearchResponse result = new SearchResponse();
            result.setTotal(response.hits().total().value());
            result.setPage(request.getPage());
            result.setPageSize(request.getPageSize());
            result.setTook(System.currentTimeMillis() - startTime);
            result.setHits(response.hits().hits().stream()
                    .map(this::convertHit)
                    .collect(Collectors.toList()));

            log.info("Search completed: appKey={}, query={}, total={}, took={}ms",
                    request.getAppKey(), request.getQuery(), result.getTotal(), result.getTook());

            return result;

        } catch (Exception e) {
            log.error("Search failed: appKey={}, query={}", request.getAppKey(), request.getQuery(), e);
            throw new RuntimeException("Search failed", e);
        }
    }

    /**
     * Build the query DSL from search request
     */
    private org.opensearch.client.opensearch._types.query_dsl.Query buildQuery(
            SearchRequest request,
            org.opensearch.client.opensearch._types.query_dsl.Query.Builder queryBuilder) {

        if (request.getQuery() == null || request.getQuery().isEmpty()) {
            return queryBuilder.matchAll(m -> m).build();
        }

        // Build a simple string query for text search
        return queryBuilder.simpleString(s -> s
                .fields("title^2", "description", "content")
                .query(request.getQuery())
        ).build();
    }

    /**
     * Convert OpenSearch hit to SearchResponse hit
     */
    private SearchResponse.Hit convertHit(Map<String, Object> osHit) {
        // Note: The actual Hit type from OpenSearch has more structure
        // This is simplified for the basic implementation
        SearchResponse.Hit hit = new SearchResponse.Hit();
        // In real implementation, extract from proper Hit object
        hit.setSource(osHit);
        return hit;
    }

    /**
     * Get index name for app key
     */
    private String getIndexName(String appKey) {
        if (appKey == null || appKey.isEmpty()) {
            return indexPrefix + "_default";
        }
        return indexPrefix + "_" + appKey.toLowerCase();
    }
}
