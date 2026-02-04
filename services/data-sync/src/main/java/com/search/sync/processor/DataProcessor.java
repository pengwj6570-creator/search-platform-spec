package com.search.sync.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.search.sync.writer.ESWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

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

            esWriter.upsert(table, id, document);
            log.debug("Upserted document: table={}, id={}", table, id);

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
}
