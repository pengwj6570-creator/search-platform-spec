package com.search.sync.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.search.sync.vectorization.VectorizationQueue;
import com.search.sync.vectorization.VectorizationTask;
import com.search.sync.writer.ESWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Processor for data change events
 *
 * Consumes CDC event JSON, extracts operation type and data,
 * and delegates to the appropriate ES writer operation.
 */
@Component
public class DataProcessor {

    private static final Logger log = LoggerFactory.getLogger(DataProcessor.class);

    private final ObjectMapper mapper;
    private final ESWriter esWriter;

    @Autowired(required = false)
    private VectorizationQueue vectorizationQueue;

    @Value("${vectorization.async.enabled:true}")
    private boolean vectorizationEnabled;

    public DataProcessor(ESWriter esWriter) {
        this.mapper = new ObjectMapper();
        this.esWriter = esWriter;
    }

    /**
     * Process a CDC event message
     *
     * @param message JSON string of the CDC event
     */
    public void process(String message) {
        if (message == null || message.isEmpty()) {
            log.warn("Received empty message");
            return;
        }

        try {
            JsonNode event = mapper.readTree(message);

            // Extract operation type
            String op = event.has("op") ? event.get("op").asText() : null;
            if (op == null) {
                log.warn("Event missing operation type: {}", message);
                return;
            }

            // Extract source information
            JsonNode source = event.get("source");
            if (source == null) {
                log.warn("Event missing source node: {}", message);
                return;
            }

            String table = source.has("table") ? source.get("table").asText() : null;
            if (table == null) {
                log.warn("Event missing table name: {}", message);
                return;
            }

            // Process based on operation type
            switch (op) {
                case "c": // Create
                case "u": // Update
                    handleUpsert(event, table);
                    break;

                case "d": // Delete
                    handleDelete(event, table);
                    break;

                case "r": // Read (snapshot)
                    handleRead(event, table);
                    break;

                default:
                    log.warn("Unknown operation type: {}", op);
            }

        } catch (Exception e) {
            log.error("Failed to process message: {}", message, e);
        }
    }

    /**
     * Handle insert or update operation
     *
     * @param event the CDC event
     * @param table source table name
     */
    private void handleUpsert(JsonNode event, String table) {
        try {
            JsonNode after = event.get("after");
            if (after == null || after.isNull()) {
                log.warn("Upsert event missing 'after' node for table: {}", table);
                return;
            }

            String document = after.toString();
            String id = extractId(after);

            // Convert to Map for field extraction
            Map<String, Object> docData = convertNodeToMap(after);

            esWriter.upsert(table, id, document);
            log.debug("Upserted document: table={}, id={}", table, id);

            // Check for vectorization needs (bypass mode)
            if (vectorizationEnabled && vectorizationQueue != null) {
                enqueueVectorizationTasks(table, id, docData);
            }

        } catch (Exception e) {
            log.error("Failed to handle upsert for table: {}", table, e);
        }
    }

    /**
     * Handle delete operation
     *
     * @param event the CDC event
     * @param table source table name
     */
    private void handleDelete(JsonNode event, String table) {
        try {
            JsonNode before = event.get("before");
            if (before == null || before.isNull()) {
                log.warn("Delete event missing 'before' node for table: {}", table);
                return;
            }

            String id = extractId(before);
            esWriter.delete(table, id);
            log.debug("Deleted document: table={}, id={}", table, id);

        } catch (Exception e) {
            log.error("Failed to handle delete for table: {}", table, e);
        }
    }

    /**
     * Handle read operation (initial snapshot)
     *
     * @param event the CDC event
     * @param table source table name
     */
    private void handleRead(JsonNode event, String table) {
        // Read events from snapshot are treated like inserts
        handleUpsert(event, table);
    }

    /**
     * Extract document ID from the JSON node
     *
     * @param node the JSON node containing document data
     * @return the document ID
     */
    private String extractId(JsonNode node) {
        // Try common ID field names
        String[] idFields = {"id", "ID", "_id", "pk", "primary_key"};

        for (String field : idFields) {
            if (node.has(field)) {
                JsonNode idNode = node.get(field);
                if (idNode.isTextual()) {
                    return idNode.asText();
                } else if (idNode.isNumber()) {
                    return String.valueOf(idNode.asLong());
                } else if (idNode.isInt()) {
                    return String.valueOf(idNode.asInt());
                }
            }
        }

        // Fallback: generate hash-based ID from content
        log.warn("Could not find ID field, using hash-based ID");
        return String.valueOf(node.toString().hashCode());
    }

    /**
     * Convert JsonNode to Map for easier field access
     *
     * @param node the JSON node
     * @return map of field names to values
     */
    private Map<String, Object> convertNodeToMap(JsonNode node) {
        Map<String, Object> map = new HashMap<>();
        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            JsonNode valueNode = entry.getValue();
            if (valueNode.isTextual()) {
                map.put(entry.getKey(), valueNode.asText());
            } else if (valueNode.isNumber()) {
                map.put(entry.getKey(), valueNode.asDouble());
            } else if (valueNode.isBoolean()) {
                map.put(entry.getKey(), valueNode.asBoolean());
            } else if (valueNode.isObject() || valueNode.isArray()) {
                map.put(entry.getKey(), valueNode.toString());
            }
        }
        return map;
    }

    /**
     * Enqueue vectorization tasks for fields that need vectorization
     *
     * This method checks document fields for vectorization configuration.
     * In production, this would load field configuration from the config service.
     *
     * @param indexName the index name
     * @param documentId the document ID
     * @param docData the document data
     */
    private void enqueueVectorizationTasks(String indexName, String documentId,
                                          Map<String, Object> docData) {
        try {
            // TODO: Load actual field configuration from config service
            // For now, use a simple heuristic: check for common text fields

            List<VectorizationConfig> vectorConfigs = detectVectorizableFields(docData);

            for (VectorizationConfig config : vectorConfigs) {
                VectorizationTask task = new VectorizationTask(
                        indexName,
                        documentId,
                        config.sourceFields,
                        config.targetField,
                        docData
                );
                vectorizationQueue.enqueue(task);
                log.debug("Enqueued vectorization task for field: {}", config.targetField);
            }

        } catch (Exception e) {
            log.error("Failed to enqueue vectorization tasks", e);
        }
    }

    /**
     * Detect fields that need vectorization
     *
     * In production, this should load from the config service.
     * For now, uses a simple heuristic approach.
     *
     * @param docData the document data
     * @return list of vectorization configurations
     */
    private List<VectorizationConfig> detectVectorizableFields(Map<String, Object> docData) {
        List<VectorizationConfig> configs = new ArrayList<>();

        // Example: if there's a 'title' and 'description' field,
        // create a combined vector for semantic search
        if (docData.containsKey("title") && docData.containsKey("description")) {
            configs.add(new VectorizationConfig(
                    List.of("title", "description"),
                    "combined_vector"
            ));
        }

        // Single field vectorization for content
        if (docData.containsKey("content")) {
            configs.add(new VectorizationConfig(
                    List.of("content"),
                    "content_vector"
            ));
        }

        return configs;
    }

    /**
     * Internal class for vectorization configuration
     */
    private static class VectorizationConfig {
        List<String> sourceFields;
        String targetField;

        VectorizationConfig(List<String> sourceFields, String targetField) {
            this.sourceFields = sourceFields;
            this.targetField = targetField;
        }
    }
}
