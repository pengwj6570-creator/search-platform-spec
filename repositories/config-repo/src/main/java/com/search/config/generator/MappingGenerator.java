package com.search.config.generator;

import com.search.config.model.FieldConfig;
import com.search.config.model.FieldType;
import com.search.config.model.SearchObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Generator for OpenSearch/Elasticsearch mapping from SearchObject configuration
 */
public class MappingGenerator {

    private static final Logger log = LoggerFactory.getLogger(MappingGenerator.class);
    private final ObjectMapper mapper;

    public MappingGenerator() {
        this.mapper = new ObjectMapper();
    }

    /**
     * Generate OpenSearch mapping JSON from SearchObject configuration
     *
     * @param object the search object configuration
     * @return JSON string of the mapping
     */
    public String generate(SearchObject object) {
        if (object == null) {
            throw new IllegalArgumentException("SearchObject cannot be null");
        }
        if (object.getFields() == null || object.getFields().isEmpty()) {
            throw new IllegalArgumentException("SearchObject must have at least one field");
        }

        Map<String, Object> mapping = new LinkedHashMap<>();
        Map<String, Object> properties = new LinkedHashMap<>();

        for (FieldConfig field : object.getFields()) {
            if (field != null && field.getName() != null) {
                properties.put(field.getName(), buildFieldMapping(field));
            }
        }

        mapping.put("properties", properties);

        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(mapping);
        } catch (Exception e) {
            log.error("Failed to generate mapping for object: {}", object.getObjectId(), e);
            throw new RuntimeException("Failed to generate mapping", e);
        }
    }

    /**
     * Build mapping configuration for a single field
     *
     * @param field the field configuration
     * @return mapping properties for the field
     */
    private Map<String, Object> buildFieldMapping(FieldConfig field) {
        Map<String, Object> mapping = new LinkedHashMap<>();
        String esType = mapFieldType(field.getType());
        mapping.put("type", esType);

        // Handle text-specific configurations
        if (field.getType() == FieldType.TEXT) {
            if (field.getAnalyzer() != null && !field.getAnalyzer().isEmpty()) {
                mapping.put("analyzer", field.getAnalyzer());
            }
            // Add fielddata for sorting/filtering on text fields
            if (field.isSortable() || field.isFilterable()) {
                mapping.put("fielddata", true);
            }
        }

        // Handle vector field configurations
        if (field.getType() == FieldType.DENSE_VECTOR) {
            if (field.getVectorDim() > 0) {
                mapping.put("dims", field.getVectorDim());
            }
            mapping.put("index", true);
            mapping.put("similarity", "cosine");
        }

        // Add boost if configured
        if (field.getBoost() > 1.0f) {
            Map<String, Object> boostMap = new HashMap<>();
            boostMap.put("boost", field.getBoost());
            mapping.put("fields", Map.of("keyword", Map.of("type", "keyword", "ignore_above", 256)));
        }

        return mapping;
    }

    /**
     * Map internal FieldType to OpenSearch field type
     *
     * @param type the internal field type
     * @return OpenSearch field type string
     */
    private String mapFieldType(FieldType type) {
        if (type == null) {
            return "text";
        }

        switch (type) {
            case TEXT:
                return "text";
            case KEYWORD:
                return "keyword";
            case INTEGER:
                return "integer";
            case LONG:
                return "long";
            case DOUBLE:
                return "double";
            case DATE:
                return "date";
            case BOOLEAN:
                return "boolean";
            case DENSE_VECTOR:
                return "knn_vector";
            default:
                return "text";
        }
    }
}
