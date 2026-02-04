package com.search.query.rerank;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Objects;

/**
 * Sort rule configuration for reranking
 *
 * Defines how to combine multiple factors to compute the final ranking score.
 */
public class SortRule {

    /**
     * Unique rule identifier
     */
    @JsonProperty("ruleId")
    private String ruleId;

    /**
     * Application key this rule belongs to
     */
    @JsonProperty("appKey")
    private String appKey;

    /**
     * List of ranking factors
     */
    @JsonProperty("factors")
    private List<Factor> factors;

    /**
     * Whether the rule is enabled
     */
    @JsonProperty("enabled")
    private boolean enabled = true;

    /**
     * Ranking factor
     */
    public static class Factor {

        /**
         * Field name to use for scoring (e.g., sales, freshness, price_score)
         */
        @JsonProperty("field")
        private String field;

        /**
         * Weight for this factor
         */
        @JsonProperty("weight")
        private double weight;

        /**
         * Scoring mode: linear, log, or custom
         */
        @JsonProperty("mode")
        private String mode = "linear";

        /**
         * Optional parameters for custom scoring
         */
        @JsonProperty("params")
        private java.util.Map<String, Object> params;

        public String getField() {
            return field;
        }

        public void setField(String field) {
            this.field = field;
        }

        public double getWeight() {
            return weight;
        }

        public void setWeight(double weight) {
            this.weight = weight;
        }

        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }

        public java.util.Map<String, Object> getParams() {
            return params;
        }

        public void setParams(java.util.Map<String, Object> params) {
            this.params = params;
        }
    }

    // Getters and Setters
    public String getRuleId() {
        return ruleId;
    }

    public void setRuleId(String ruleId) {
        this.ruleId = ruleId;
    }

    public String getAppKey() {
        return appKey;
    }

    public void setAppKey(String appKey) {
        this.appKey = appKey;
    }

    public List<Factor> getFactors() {
        return factors;
    }

    public void setFactors(List<Factor> factors) {
        this.factors = factors;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SortRule sortRule = (SortRule) o;
        return Objects.equals(ruleId, sortRule.ruleId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ruleId);
    }
}
