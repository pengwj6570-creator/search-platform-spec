package com.search.query.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * Search response model for query API
 */
public class SearchResponse {

    /**
     * Search result hits
     */
    @JsonProperty("hits")
    private List<Hit> hits;

    /**
     * Total number of matching documents
     */
    @JsonProperty("total")
    private long total;

    /**
     * Current page number
     */
    @JsonProperty("page")
    private int page;

    /**
     * Page size
     */
    @JsonProperty("pageSize")
    private int pageSize;

    /**
     * Query execution time in milliseconds
     */
    @JsonProperty("took")
    private long took;

    /**
     * Search result hit
     */
    public static class Hit {

        /**
         * Document ID
         */
        @JsonProperty("id")
        private String id;

        /**
         * Relevance score
         */
        @JsonProperty("score")
        private float score;

        /**
         * Document source data
         */
        @JsonProperty("source")
        private Map<String, Object> source;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public float getScore() {
            return score;
        }

        public void setScore(float score) {
            this.score = score;
        }

        public Map<String, Object> getSource() {
            return source;
        }

        public void setSource(Map<String, Object> source) {
            this.source = source;
        }
    }

    // Getters and Setters
    public List<Hit> getHits() {
        return hits;
    }

    public void setHits(List<Hit> hits) {
        this.hits = hits;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
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

    public long getTook() {
        return took;
    }

    public void setTook(long took) {
        this.took = took;
    }
}
