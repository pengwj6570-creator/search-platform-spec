package com.search.sync.consumer;

import com.search.sync.processor.DataProcessor;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Kafka consumer for data change events
 *
 * Consumes CDC events from Kafka and forwards them to the data processor
 * for synchronization to OpenSearch.
 */
@Component
public class DataChangeConsumer implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(DataChangeConsumer.class);

    private final String bootstrapServers;
    private final String topic;
    private final String groupId;
    private final DataProcessor processor;

    private KafkaConsumer<String, String> consumer;
    private final AtomicBoolean running = new AtomicBoolean(false);

    /**
     * Create a new data change consumer
     *
     * @param bootstrapServers Kafka bootstrap servers
     * @param topic topic to consume from
     * @param groupId consumer group ID
     * @param processor data processor for handling messages
     */
    public DataChangeConsumer(String bootstrapServers, String topic,
                               String groupId, DataProcessor processor) {
        this.bootstrapServers = bootstrapServers;
        this.topic = topic;
        this.groupId = groupId;
        this.processor = processor;
    }

    /**
     * Start the consumer
     */
    public void start() {
        if (running.getAndSet(true)) {
            log.warn("Consumer is already running");
            return;
        }

        Properties props = createConsumerProperties();
        this.consumer = new KafkaConsumer<>(props);

        Thread thread = new Thread(this, "data-change-consumer");
        thread.setDaemon(false);
        thread.start();

        log.info("Data change consumer started: topic={}, group={}", topic, groupId);
    }

    /**
     * Stop the consumer
     */
    public void stop() {
        if (!running.getAndSet(false)) {
            return;
        }

        if (consumer != null) {
            consumer.wakeup();
        }
        log.info("Data change consumer stopped");
    }

    @Override
    public void run() {
        try {
            consumer.subscribe(Collections.singletonList(topic));

            log.info("Consumer subscribed to topic: {}", topic);

            while (running.get()) {
                try {
                    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));

                    int count = 0;
                    for (ConsumerRecord<String, String> record : records) {
                        try {
                            processor.process(record.value());
                            count++;
                        } catch (Exception e) {
                            log.error("Failed to process record: offset={}, key={}",
                                    record.offset(), record.key(), e);
                            // Continue processing other records
                        }
                    }

                    if (count > 0) {
                        log.debug("Processed {} records from topic {}", count, topic);
                    }

                } catch (org.apache.kafka.common.errors.WakeupException e) {
                    // Expected when stopping
                    log.info("Consumer wakeup received");
                    break;
                } catch (Exception e) {
                    log.error("Error polling/consuming records", e);
                    // Back off before retrying
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        } finally {
            try {
                if (consumer != null) {
                    consumer.close();
                    log.info("Consumer closed");
                }
            } catch (Exception e) {
                log.error("Error closing consumer", e);
            }
        }
    }

    /**
     * Create consumer properties
     *
     * @return Kafka consumer properties
     */
    private Properties createConsumerProperties() {
        Properties props = new Properties();

        // Basic configuration
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");

        // Deserializers
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        // Performance tuning
        props.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, "1024");
        props.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, "500");
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "100");
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "30000");

        return props;
    }

    /**
     * Check if the consumer is running
     *
     * @return true if running, false otherwise
     */
    public boolean isRunning() {
        return running.get();
    }
}
