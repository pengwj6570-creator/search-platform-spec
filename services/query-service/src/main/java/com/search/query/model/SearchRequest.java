package com.search.query.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * Search request model for query API
 */
public class SearchRequest {

    /**
     * Search query string
     */
    @JsonProperty("query")
    private String query;

    /**
     * Application key for multi-tenancy
     */
    @JsonProperty("appKey")
    private String appKey;

    /**
     * Recall strategy configuration
     */
    @JsonProperty("recallStrategy")
    private RecallStrategy recallStrategy;

    /**
     * Filter criteria
     */
    @JsonProperty("filters")
    private Map<String, Object> filters;

    /**
     * Sort configuration
     */
    @JsonProperty("sort")
    private Sort sort;

    /**
     * Page number (1-based)
     */
    @JsonProperty("page")
    private int page = 1;

    /**
     * Page size
     */
    @JsonProperty("pageSize")
    private int pageSize = 20;

    /**
     * Recall strategy configuration
     */
    public static class RecallStrategy {

        /**
         * Enable keyword recall
         */
        @JsonProperty("keyword")
        private boolean keyword = true;

        /**
         * Vector recall configuration
         */
        @JsonProperty("vector")
        private VectorConfig vector;

        /**
         * Enable hot/popular items recall
         */
        @JsonProperty("hot")
        private boolean hot = true;

        public boolean isKeyword() {
            return keyword;
        }

        public void setKeyword(boolean keyword) {
            this.keyword = keyword;
        }

        public VectorConfig getVector() {
            return vector;
        }

        public void setVector(VectorConfig vector) {
            this.vector = vector;
        }

        public boolean isHot() {
            return hot;
        }

        public void setHot(boolean hot) {
            this.hot = hot;
        }
    }

    /**
     * Vector recall configuration
     */
    public static class VectorConfig {

        /**
         * Enable vector recall
         */
        @JsonProperty("enabled")
        private boolean enabled;

        /**
         * Weight for vector recall score fusion
         */
        @JsonProperty("weight")
        private double weight = 0.3;

        /**
         * Number of candidates to recall (k)
         */
        @JsonProperty("k")
        private int k = 100;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public double getWeight() {
            return weight;
        }

        public void setWeight(double weight) {
            this.weight = weight;
        }

        public int getK() {
            return k;
        }

        public void setK(int k) {
            this.k = k;
        }
    }

    /**
     * Sort configuration
     */
    public static class Sort {

        /**
         * Field to sort by
         */
        @JsonProperty("field")
        private String field;

        /**
         * Sort order: asc or desc
         */
        @JsonProperty("order")
        private String order = "desc";

        public String getField() {
            return field;
        }

        public void setField(String field) {
            this.field = field;
        }

        public String getOrder() {
            return order;
        }

        public void setOrder(String order) {
            this.order = order;
        }
    }

    // Getters and Setters
    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getAppKey() {
        return appKey;
    }

    public void setAppKey(String appKey) {
        this.appKey = appKey;
    }

    public RecallStrategy getRecallStrategy() {
        return recallStrategy;
    }

    public void setRecallStrategy(RecallStrategy recallStrategy) {
        this.recallStrategy = recallStrategy;
    }

    public Map<String, Object> getFilters() {
        return filters;
    }

    public void setFilters(Map<String, Object> filters) {
        this.filters = filters;
    }

    public Sort getSort() {
        return sort;
    }

    public void setSort(Sort sort) {
        this.sort = sort;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }
}
