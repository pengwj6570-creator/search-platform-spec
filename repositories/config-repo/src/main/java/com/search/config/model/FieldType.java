package com.search.config.model;

/**
 * Field type enumeration for search index configuration
 */
public enum FieldType {
    /**
     * Text field for full-text search
     */
    TEXT,

    /**
     * Keyword field for exact matching
     */
    KEYWORD,

    /**
     * Integer number field
     */
    INTEGER,

    /**
     * Long integer number field
     */
    LONG,

    /**
     * Double precision floating point number field
     */
    DOUBLE,

    /**
     * Date/time field
     */
    DATE,

    /**
     * Boolean field
     */
    BOOLEAN,

    /**
     * Dense vector field for semantic search
     */
    DENSE_VECTOR
}
