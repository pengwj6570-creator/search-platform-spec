package com.search.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Search object configuration defining what data to index
 */
public class SearchObject {

    /**
     * Unique identifier for the search object
     */
    @JsonProperty("objectId")
    private String objectId;

    /**
     * Reference to the data source ID
     */
    @JsonProperty("sourceId")
    private String sourceId;

    /**
     * Table name or collection name
     */
    @JsonProperty("table")
    private String table;

    /**
     * Primary key field name
     */
    @JsonProperty("primaryKey")
    private String primaryKey;

    /**
     * List of field configurations
     */
    @JsonProperty("fields")
    private List<FieldConfig> fields;

    /**
     * Application key for multi-tenancy
     */
    @JsonProperty("appKey")
    private String appKey;

    public SearchObject() {
        this.fields = new ArrayList<>();
    }

    public SearchObject(String objectId, String sourceId) {
        this.objectId = objectId;
        this.sourceId = sourceId;
        this.fields = new ArrayList<>();
    }

    public String getObjectId() {
        return objectId;
    }

    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }

    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    public String getTable() {
        return table;
    }

    public void setTable(String table) {
        this.table = table;
    }

    public String getPrimaryKey() {
        return primaryKey;
    }

    public void setPrimaryKey(String primaryKey) {
        this.primaryKey = primaryKey;
    }

    public List<FieldConfig> getFields() {
        return fields;
    }

    public void setFields(List<FieldConfig> fields) {
        this.fields = fields;
    }

    public String getAppKey() {
        return appKey;
    }

    public void setAppKey(String appKey) {
        this.appKey = appKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SearchObject that = (SearchObject) o;
        return Objects.equals(objectId, that.objectId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(objectId);
    }

    @Override
    public String toString() {
        return "SearchObject{" +
                "objectId='" + objectId + '\'' +
                ", sourceId='" + sourceId + '\'' +
                ", table='" + table + '\'' +
                ", primaryKey='" + primaryKey + '\'' +
                ", fields=" + fields +
                ", appKey='" + appKey + '\'' +
                '}';
    }
}
