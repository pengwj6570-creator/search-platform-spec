package com.search.query.recall;

import com.search.query.model.SearchRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Recall engine for orchestrating multi-path recall
 *
 * Executes keyword, vector, and hot recall in parallel and merges results.
 */
@Component
public class RecallEngine {

    private static final Logger log = LoggerFactory.getLogger(RecallEngine.class);

    private final KeywordRecall keywordRecall;
    private final VectorRecall vectorRecall;
    private final HotRecall hotRecall;
    private final ExecutorService executor;

    public RecallEngine(KeywordRecall keywordRecall, VectorRecall vectorRecall, HotRecall hotRecall) {
        this.keywordRecall = keywordRecall;
        this.vectorRecall = vectorRecall;
        this.hotRecall = hotRecall;
        this.executor = Executors.newFixedThreadPool(3);
    }

    /**
     * Execute multi-path recall
     *
     * @param index the OpenSearch index name
     * @param request the search request
     * @return fused recall results
     */
    public List<RecallResult> recall(String index, SearchRequest request) {
        SearchRequest.RecallStrategy strategy = request.getRecallStrategy();
        if (strategy == null) {
            // Default to keyword only
            strategy = new SearchRequest.RecallStrategy();
        }

        List<CompletableFuture<List<RecallResult>>> futures = new ArrayList<>();

        // Keyword recall
        if (strategy.isKeyword() && request.getQuery() != null) {
            futures.add(CompletableFuture.supplyAsync(() -> {
                log.debug("Starting keyword recall");
                if (request.getFilters() != null && !request.getFilters().isEmpty()) {
                    return keywordRecall.recallWithFilters(index, request.getQuery(), request.getFilters(), 100);
                }
                return keywordRecall.recall(index, request.getQuery(), 100);
            }, executor));
        }

        // Vector recall
        if (strategy.getVector() != null && strategy.getVector().isEnabled() && request.getQuery() != null) {
            futures.add(CompletableFuture.supplyAsync(() -> {
                log.debug("Starting vector recall");
                int k = strategy.getVector().getK();
                return vectorRecall.recall(index, request.getQuery(), "title_vector", k);
            }, executor));
        }

        // Hot recall
        if (strategy.isHot()) {
            futures.add(CompletableFuture.supplyAsync(() -> {
                log.debug("Starting hot recall");
                return hotRecall.recall(index, "sales", 50);
            }, executor));
        }

        // Wait for all recall operations and merge results
        CompletableFuture<Void> allOf = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0])
        );

        try {
            allOf.join();

            List<RecallResult> allResults = new ArrayList<>();
            for (CompletableFuture<List<RecallResult>> future : futures) {
                allResults.addAll(future.get());
            }

            log.info("Multi-path recall completed: total results={}", allResults.size());
            return allResults;

        } catch (Exception e) {
            log.error("Recall execution failed", e);
            return List.of();
        }
    }

    /**
     * Execute single-path keyword recall
     *
     * @param index the OpenSearch index name
     * @param query the search query
     * @param filters filter criteria
     * @param topK number of results to return
     * @return recall results
     */
    public List<RecallResult> keywordRecall(String index, String query, Map<String, Object> filters, int topK) {
        if (filters != null && !filters.isEmpty()) {
            return keywordRecall.recallWithFilters(index, query, filters, topK);
        }
        return keywordRecall.recall(index, query, topK);
    }

    /**
     * Execute single-path vector recall
     *
     * @param index the OpenSearch index name
     * @param query the search query
     * @param vectorField the vector field name
     * @param topK number of results to return
     * @return recall results
     */
    public List<RecallResult> vectorRecall(String index, String query, String vectorField, int topK) {
        return vectorRecall.recall(index, query, vectorField, topK);
    }

    /**
     * Execute single-path hot recall
     *
     * @param index the OpenSearch index name
     * @param sortField the field to sort by
     * @param topK number of results to return
     * @return recall results
     */
    public List<RecallResult> hotRecall(String index, String sortField, int topK) {
        return hotRecall.recall(index, sortField, topK);
    }

    /**
     * Shutdown the executor
     */
    public void shutdown() {
        executor.shutdown();
    }
}
