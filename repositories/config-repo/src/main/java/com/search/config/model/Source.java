package com.search.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import java.util.Objects;

/**
 * Data source configuration
 */
public class Source {

    /**
     * Unique identifier for the source
     */
    @JsonProperty("sourceId")
    private String sourceId;

    /**
     * Source type (database or file)
     */
    @JsonProperty("sourceType")
    private SourceType sourceType;

    /**
     * Connection string or file path
     */
    @JsonProperty("connection")
    private String connection;

    /**
     * Additional properties as key-value pairs
     */
    @JsonProperty("properties")
    private Map<String, String> properties;

    public Source() {
    }

    public Source(String sourceId, SourceType sourceType) {
        this.sourceId = sourceId;
        this.sourceType = sourceType;
    }

    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    public SourceType getSourceType() {
        return sourceType;
    }

    public void setSourceType(SourceType sourceType) {
        this.sourceType = sourceType;
    }

    public String getConnection() {
        return connection;
    }

    public void setConnection(String connection) {
        this.connection = connection;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Source source = (Source) o;
        return Objects.equals(sourceId, source.sourceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sourceId);
    }

    @Override
    public String toString() {
        return "Source{" +
                "sourceId='" + sourceId + '\'' +
                ", sourceType=" + sourceType +
                ", connection='" + connection + '\'' +
                ", properties=" + properties +
                '}';
    }
}
