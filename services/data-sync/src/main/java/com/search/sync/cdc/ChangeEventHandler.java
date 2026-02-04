package com.search.sync.cdc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.debezium.engine.ChangeEvent;
import io.debezium.engine.DebeziumEngine;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Handler for processing Debezium change events
 *
 * Captures change events from database CDC and publishes them to Kafka
 * for downstream processing by the data sync service.
 */
public class ChangeEventHandler implements DebeziumEngine.Handler<ChangeEvent<String, String>> {

    private static final Logger log = LoggerFactory.getLogger(ChangeEventHandler.class);

    private final KafkaProducer<String, String> producer;
    private final String topic;
    private final ObjectMapper mapper;

    /**
     * Create a new change event handler
     *
     * @param producer Kafka producer for publishing events
     * @param topic topic name for change events
     */
    public ChangeEventHandler(KafkaProducer<String, String> producer, String topic) {
        this.producer = producer;
        this.topic = topic;
        this.mapper = new ObjectMapper();
    }

    @Override
    public void handle(List<ChangeEvent<String, String>> events) {
        for (ChangeEvent<String, String> event : events) {
            try {
                processEvent(event);
            } catch (Exception e) {
                log.error("Failed to process change event: key={}", event.key(), e);
            }
        }
    }

    /**
     * Process a single change event
     *
     * @param event the change event
     */
    private void processEvent(ChangeEvent<String, String> event) {
        try {
            String key = event.key();
            String value = event.value();

            if (value == null || value.isEmpty()) {
                log.debug("Skipping event with empty value, key={}", key);
                return;
            }

            // Parse the event to extract metadata
            JsonNode eventNode = mapper.readTree(value);
            JsonNode sourceNode = eventNode.get("source");

            if (sourceNode == null) {
                log.warn("Event missing 'source' node, key={}", key);
                return;
            }

            // Extract source table information
            String table = sourceNode.has("table") ? sourceNode.get("table").asText() : "unknown";
            String operation = eventNode.has("op") ? eventNode.get("op").asText() : "unknown";

            // Enrich the event with metadata
            String enrichedValue = enrichEvent(value, table, operation);

            // Send to Kafka
            ProducerRecord<String, String> record = new ProducerRecord<>(
                    topic,
                    key,
                    enrichedValue
            );

            producer.send(record, (metadata, exception) -> {
                if (exception != null) {
                    log.error("Failed to send event to Kafka: key={}, table={}",
                            key, table, exception);
                } else {
                    log.debug("Sent change event to Kafka: key={}, table={}, op={}, partition={}, offset={}",
                            key, table, operation, metadata.partition(), metadata.offset());
                }
            });

        } catch (Exception e) {
            log.error("Error processing change event: key={}", event.key(), e);
        }
    }

    /**
     * Enrich the event with additional metadata
     *
     * @param value original event JSON
     * @param table source table name
     * @param operation operation type (c=create, r=read, u=update, d=delete)
     * @return enriched event JSON
     */
    private String enrichEvent(String value, String table, String operation) {
        // In a production system, you might add additional metadata here
        // such as timestamps, source system identifiers, etc.
        return value;
    }

    /**
     * Get the topic name for this handler
     *
     * @return topic name
     */
    public String getTopic() {
        return topic;
    }
}
