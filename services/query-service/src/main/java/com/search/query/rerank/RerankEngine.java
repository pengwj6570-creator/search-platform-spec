package com.search.query.rerank;

import com.search.query.recall.RecallResult;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.GetResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Rerank engine for fine-tuning search results
 *
 * Applies configurable ranking factors to reorder recall results.
 */
@Component
public class RerankEngine {

    private static final Logger log = LoggerFactory.getLogger(RerankEngine.class);

    private final OpenSearchClient client;
    private final SortRuleLoader ruleLoader;
    private final String indexPrefix;

    public RerankEngine(OpenSearchClient client, SortRuleLoader ruleLoader) {
        this.client = client;
        this.ruleLoader = ruleLoader;
        this.indexPrefix = "search";
    }

    /**
     * Rerank candidates using the configured rule for the app
     *
     * @param appKey the application key
     * @param candidates the recall results to rerank
     * @return reranked results
     */
    public List<RecallResult> rerank(String appKey, List<RecallResult> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }

        SortRule rule = ruleLoader.getRule(appKey);
        if (rule == null || rule.getFactors() == null || rule.getFactors().isEmpty()) {
            log.debug("No rerank rule found for appKey: {}, returning original results", appKey);
            return candidates;
        }

        // Fetch documents for scoring
        Map<String, Map<String, Object>> docs = fetchDocuments(appKey, candidates);

        // Calculate new scores and rerank
        List<RecallResult> reranked = candidates.stream()
                .map(result -> {
                    Map<String, Object> doc = docs.get(result.getId());
                    float newScore = calculateScore(result, doc, rule);
                    return new RecallResult(result.getId(), newScore, "rerank");
                })
                .sorted((a, b) -> Float.compare(b.getScore(), a.getScore()))
                .collect(Collectors.toList());

        log.info("Reranked {} candidates for appKey: {}", reranked.size(), appKey);
        return reranked;
    }

    /**
     * Rerank with a specific rule
     *
     * @param rule the sort rule to apply
     * @param candidates the recall results to rerank
     * @param appKey the application key (for index lookup)
     * @return reranked results
     */
    public List<RecallResult> rerankWithRule(SortRule rule, List<RecallResult> candidates, String appKey) {
        if (rule == null || !rule.isEnabled()) {
            return candidates;
        }

        Map<String, Map<String, Object>> docs = fetchDocuments(appKey, candidates);

        return candidates.stream()
                .map(result -> {
                    Map<String, Object> doc = docs.get(result.getId());
                    float newScore = calculateScore(result, doc, rule);
                    return new RecallResult(result.getId(), newScore, "rerank");
                })
                .sorted((a, b) -> Float.compare(b.getScore(), a.getScore()))
                .collect(Collectors.toList());
    }

    /**
     * Calculate the final score for a result
     *
     * @param result the recall result
     * @param doc the document data
     * @param rule the sort rule
     * @return final score
     */
    private float calculateScore(RecallResult result, Map<String, Object> doc, SortRule rule) {
        // Start with the base recall score
        float baseScore = result.getScore();
        float totalScore = baseScore;

        // Apply each factor
        for (SortRule.Factor factor : rule.getFactors()) {
            String field = factor.getField();

            // Special handling for _score (the original relevance score)
            if ("_score".equals(field)) {
                totalScore += baseScore * (float) factor.getWeight();
                continue;
            }

            // Get the field value from document
            Object value = doc != null ? doc.get(field) : null;
            if (value == null) {
                continue;
            }

            // Calculate factor score based on mode
            double factorScore = calculateFactorScore(value, factor);

            // Add weighted score
            totalScore += factorScore * factor.getWeight();
        }

        return totalScore;
    }

    /**
     * Calculate the score contribution from a single factor
     *
     * @param value the field value
     * @param factor the factor configuration
     * @return the factor score
     */
    private double calculateFactorScore(Object value, SortRule.Factor factor) {
        if (value == null) {
            return 0;
        }

        double rawValue;

        if (value instanceof Number) {
            rawValue = ((Number) value).doubleValue();
        } else if (value instanceof String) {
            try {
                rawValue = Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                return 0;
            }
        } else {
            return 0;
        }

        // Apply mode transformation
        switch (factor.getMode().toLowerCase()) {
            case "log":
                return Math.log1p(Math.max(0, rawValue));

            case "linear":
            default:
                return rawValue;
        }
    }

    /**
     * Fetch documents from OpenSearch for the given candidates
     *
     * @param appKey the application key
     * @param candidates the recall results
     * @return map of document ID to document source
     */
    private Map<String, Map<String, Object>> fetchDocuments(String appKey, List<RecallResult> candidates) {
        Map<String, Map<String, Object>> docs = new HashMap<>();
        String index = indexPrefix + "_" + (appKey != null ? appKey.toLowerCase() : "default");

        for (RecallResult candidate : candidates) {
            try {
                GetResponse<Map> response = client.get(g -> g
                        .index(index)
                        .id(candidate.getId()),
                        Map.class
                );

                if (response.found()) {
                    docs.put(candidate.getId(), response.source());
                }

            } catch (Exception e) {
                log.warn("Failed to fetch document: id={}, index={}", candidate.getId(), index, e);
            }
        }

        return docs;
    }
}
