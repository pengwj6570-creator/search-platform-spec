package com.search.query.recall;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Fusion module for merging multi-path recall results
 *
 * Combines results from keyword, vector, and hot recall paths,
 * normalizes scores, and applies fusion strategies.
 */
@Component
public class RecallFusion {

    private static final Logger log = LoggerFactory.getLogger(RecallFusion.class);

    /**
     * Fusion configuration
     */
    public static class FusionConfig {
        private double keywordWeight = 0.5;
        private double vectorWeight = 0.3;
        private double hotWeight = 0.2;
        private int topK = 100;

        public FusionConfig() {}

        public FusionConfig(double keywordWeight, double vectorWeight, double hotWeight, int topK) {
            this.keywordWeight = keywordWeight;
            this.vectorWeight = vectorWeight;
            this.hotWeight = hotWeight;
            this.topK = topK;
        }

        public double getKeywordWeight() { return keywordWeight; }
        public void setKeywordWeight(double keywordWeight) { this.keywordWeight = keywordWeight; }
        public double getVectorWeight() { return vectorWeight; }
        public void setVectorWeight(double vectorWeight) { this.vectorWeight = vectorWeight; }
        public double getHotWeight() { return hotWeight; }
        public void setHotWeight(double hotWeight) { this.hotWeight = hotWeight; }
        public int getTopK() { return topK; }
        public void setTopK(int topK) { this.topK = topK; }
    }

    /**
     * Fuse results from multiple recall paths
     *
     * @param keywordResults results from keyword recall
     * @param vectorResults results from vector recall
     * @param hotResults results from hot recall
     * @param config fusion configuration
     * @return fused and ranked results
     */
    public List<RecallResult> fuse(List<RecallResult> keywordResults,
                                    List<RecallResult> vectorResults,
                                    List<RecallResult> hotResults,
                                    FusionConfig config) {
        // Deduplicate and merge using LinkedHashMap to preserve insertion order
        Map<String, FusedResult> merged = new LinkedHashMap<>();

        // Merge each recall path with its weight
        mergeResults(merged, keywordResults, config.getKeywordWeight(), "keyword");
        mergeResults(merged, vectorResults, config.getVectorWeight(), "vector");
        mergeResults(merged, hotResults, config.getHotWeight(), "hot");

        // Sort by fused score
        return merged.values().stream()
                .sorted((a, b) -> Float.compare(b.score, a.score))
                .limit(config.getTopK())
                .map(fr -> new RecallResult(fr.id, fr.score, "fusion"))
                .collect(Collectors.toList());
    }

    /**
     * Fuse results using RRF (Reciprocal Rank Fusion)
     *
     * @param results list of result lists from different recall paths
     * @param k RRF constant (typically 60)
     * @return fused results
     */
    public List<RecallResult> fuseRRF(List<List<RecallResult>> results, int k) {
        Map<String, FusedResult> merged = new LinkedHashMap<>();

        for (List<RecallResult> pathResults : results) {
            for (int i = 0; i < pathResults.size(); i++) {
                RecallResult result = pathResults.get(i);
                float rrfScore = 1.0f / (k + i + 1);

                merged.merge(result.getId(),
                        new FusedResult(result.getId(), rrfScore),
                        (existing, newVal) -> {
                            existing.score += rrfScore;
                            return existing;
                        });
            }
        }

        return merged.values().stream()
                .sorted((a, b) -> Float.compare(b.score, a.score))
                .map(fr -> new RecallResult(fr.id, fr.score, "rrf"))
                .collect(Collectors.toList());
    }

    /**
     * Merge results from a single recall path
     */
    private void mergeResults(Map<String, FusedResult> merged,
                              List<RecallResult> results,
                              double weight,
                              String source) {
        if (results.isEmpty()) {
            return;
        }

        // Find max score for normalization
        float maxScore = results.stream()
                .map(RecallResult::getScore)
                .max(Float::compare)
                .orElse(1.0f);

        if (maxScore == 0) {
            maxScore = 1.0f;
        }

        for (RecallResult result : results) {
            String id = result.getId();
            // Normalize and apply weight
            float normalizedScore = (result.getScore() / maxScore) * (float) weight;

            merged.merge(id, new FusedResult(id, normalizedScore),
                    (existing, newVal) -> {
                        existing.score += normalizedScore;
                        return existing;
                    });
        }

        log.debug("Merged {} results from {}: weight={}", results.size(), source, weight);
    }

    /**
     * Internal class for tracking fused results
     */
    private static class FusedResult {
        String id;
        float score;

        FusedResult(String id, float score) {
            this.id = id;
            this.score = score;
        }
    }
}
