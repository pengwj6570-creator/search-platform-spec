package com.search.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Objects;

/**
 * Field configuration for search object metadata
 */
public class FieldConfig {

    /**
     * Field name
     */
    @JsonProperty("name")
    private String name;

    /**
     * Field data type
     */
    @JsonProperty("type")
    private FieldType type;

    /**
     * Whether the field is searchable
     */
    @JsonProperty("searchable")
    private boolean searchable;

    /**
     * Whether the field can be used for filtering
     */
    @JsonProperty("filterable")
    private boolean filterable;

    /**
     * Whether the field can be used for sorting
     */
    @JsonProperty("sortable")
    private boolean sortable;

    /**
     * Analyzer for text processing (e.g., ik_max_word, standard)
     */
    @JsonProperty("analyzer")
    private String analyzer;

    /**
     * Whether to generate vector embedding for this field
     */
    @JsonProperty("vectorize")
    private boolean vectorize;

    /**
     * Vector field type (e.g., dense_vector)
     */
    @JsonProperty("vectorType")
    private String vectorType;

    /**
     * Vector dimension for embedding
     */
    @JsonProperty("vectorDim")
    private int vectorDim;

    /**
     * For vector fields: the source field(s) to combine for vectorization.
     * If null, uses the current field's value.
     * Examples:
     * - ["title"] → use title field content
     * - ["title", "description"] → combine title and description for embedding
     */
    @JsonProperty("vectorSourceFields")
    private List<String> vectorSourceFields;

    /**
     * Vector field target name where this vector will be stored.
     * Useful when multiple source fields combine into one vector field.
     * If null, the vector is stored in a field named "{fieldName}_vector".
     */
    @JsonProperty("vectorTargetField")
    private String vectorTargetField;

    /**
     * Boost factor for relevance scoring
     */
    @JsonProperty("boost")
    private float boost;

    public FieldConfig() {
    }

    public FieldConfig(String name, FieldType type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public FieldType getType() {
        return type;
    }

    public void setType(FieldType type) {
        this.type = type;
    }

    public boolean isSearchable() {
        return searchable;
    }

    public void setSearchable(boolean searchable) {
        this.searchable = searchable;
    }

    public boolean isFilterable() {
        return filterable;
    }

    public void setFilterable(boolean filterable) {
        this.filterable = filterable;
    }

    public boolean isSortable() {
        return sortable;
    }

    public void setSortable(boolean sortable) {
        this.sortable = sortable;
    }

    public String getAnalyzer() {
        return analyzer;
    }

    public void setAnalyzer(String analyzer) {
        this.analyzer = analyzer;
    }

    public boolean isVectorize() {
        return vectorize;
    }

    public void setVectorize(boolean vectorize) {
        this.vectorize = vectorize;
    }

    public String getVectorType() {
        return vectorType;
    }

    public void setVectorType(String vectorType) {
        this.vectorType = vectorType;
    }

    public int getVectorDim() {
        return vectorDim;
    }

    public void setVectorDim(int vectorDim) {
        this.vectorDim = vectorDim;
    }

    public List<String> getVectorSourceFields() {
        return vectorSourceFields;
    }

    public void setVectorSourceFields(List<String> vectorSourceFields) {
        this.vectorSourceFields = vectorSourceFields;
    }

    public String getVectorTargetField() {
        return vectorTargetField;
    }

    public void setVectorTargetField(String vectorTargetField) {
        this.vectorTargetField = vectorTargetField;
    }

    public float getBoost() {
        return boost;
    }

    public void setBoost(float boost) {
        this.boost = boost;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FieldConfig that = (FieldConfig) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "FieldConfig{" +
                "name='" + name + '\'' +
                ", type=" + type +
                ", searchable=" + searchable +
                ", filterable=" + filterable +
                ", sortable=" + sortable +
                ", analyzer='" + analyzer + '\'' +
                ", vectorize=" + vectorize +
                ", vectorType='" + vectorType + '\'' +
                ", vectorDim=" + vectorDim +
                ", vectorSourceFields=" + vectorSourceFields +
                ", vectorTargetField='" + vectorTargetField + '\'' +
                ", boost=" + boost +
                '}';
    }
}
