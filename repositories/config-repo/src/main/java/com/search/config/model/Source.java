package com.search.config.model;

import java.util.Objects;

/**
 * Data source configuration
 */
public class Source {

    /**
     * Unique identifier for the source
     */
    private String sourceId;

    /**
     * Source type (database or file)
     */
    private SourceType sourceType;

    /**
     * Connection string or file path
     */
    private String connection;

    /**
     * Additional properties as key-value pairs
     */
    private String properties;

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

    public String getProperties() {
        return properties;
    }

    public void setProperties(String properties) {
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
                ", properties='" + properties + '\'' +
                '}';
    }
}
