package com.search.sync.cdc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Consumer;

/**
 * Handler for processing Debezium change events
 *
 * Captures change events from database CDC and publishes them to Kafka
 * for downstream processing by the data sync service.
 */
public class ChangeEventHandler implements Consumer<List<String>> {

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

    /**
     * Handle a batch of change events - Consumer interface method
     */
    @Override
    public void accept(List<String> events) {
        for (String event : events) {
            try {
                processEvent(event);
            } catch (Exception e) {
                log.error("Failed to process change event", e);
            }
        }
    }

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

    /**
     * Handle a batch of change events
     */
    @Override
    public void accept(java.util.List<String> events) {
        for (String event : events) {
            try {
                processEvent(event);
            } catch (Exception e) {
                log.error("Failed to process change event", e);
            }
        }
    }

    /**
     * Process a single change event
     *
     * @param eventJson the change event JSON string
     */
    private void processEvent(String eventJson) {
        try {
            if (eventJson == null || eventJson.isEmpty()) {
                log.debug("Skipping event with empty value");
                return;
            }

            // Parse the event to extract metadata
            JsonNode eventNode = mapper.readTree(eventJson);

            // Extract schema and payload information from Json format
            JsonNode payloadNode = eventNode.has("payload") ? eventNode.get("payload") : eventNode;

            if (!payloadNode.has("source")) {
                log.warn("Event missing 'source' node");
                return;
            }

            JsonNode sourceNode = payloadNode.get("source");

            // Extract source table information
            String table = sourceNode.has("table") ? sourceNode.get("table").asText() : "unknown";
            String operation = payloadNode.has("op") ? payloadNode.get("op").asText() : "unknown";

            // Extract key for Kafka partitioning
            String key = extractKey(eventNode, payloadNode);

            // Send to Kafka
            ProducerRecord<String, String> record = new ProducerRecord<>(
                    topic,
                    key,
                    eventJson
            );

            producer.send(record, (metadata, exception) -> {
                if (exception != null) {
                    log.error("Failed to send event to Kafka: table={}",
                            table, exception);
                } else {
                    log.debug("Sent change event to Kafka: table={}, op={}, partition={}, offset={}",
                            table, operation, metadata.partition(), metadata.offset());
                }
            });

        } catch (Exception e) {
            log.error("Error processing change event", e);
        }
    }

    /**
     * Extract key from event for Kafka partitioning
     *
     * @param eventNode full event node
     * @param payloadNode payload node
     * @return key string
     */
    private String extractKey(JsonNode eventNode, JsonNode payloadNode) {
        // Try to get the key from the schema/payload structure
        if (eventNode.has("schema") && eventNode.has("payload")) {
            JsonNode payload = eventNode.get("payload");
            if (payload.has("before") && payload.get("before") != null && !payload.get("before").isNull()) {
                JsonNode before = payload.get("before");
                if (before.has("id")) {
                    return before.get("id").asText();
                }
            } else if (payload.has("after") && payload.get("after") != null && !payload.get("after").isNull()) {
                JsonNode after = payload.get("after");
                if (after.has("id")) {
                    return after.get("id").asText();
                }
            }
        }
        // Fallback to source table name
        if (payloadNode.has("source") && payloadNode.get("source").has("table")) {
            return payloadNode.get("source").get("table").asText();
        }
        return "unknown";
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
