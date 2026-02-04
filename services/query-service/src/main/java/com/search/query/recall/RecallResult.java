package com.search.query.recall;

import java.util.Objects;

/**
 * Result from a recall operation
 *
 * Represents a single candidate document from a recall source
 * with its relevance score.
 */
public class RecallResult {

    /**
     * Document ID
     */
    private final String id;

    /**
     * Relevance score
     */
    private final float score;

    /**
     * Source recall path (keyword, vector, hot)
     */
    private final String source;

    public RecallResult(String id, float score, String source) {
        this.id = id;
        this.score = score;
        this.source = source;
    }

    public String getId() {
        return id;
    }

    public float getScore() {
        return score;
    }

    public String getSource() {
        return source;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RecallResult that = (RecallResult) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "RecallResult{" +
                "id='" + id + '\'' +
                ", score=" + score +
                ", source='" + source + '\'' +
                '}';
    }
}
