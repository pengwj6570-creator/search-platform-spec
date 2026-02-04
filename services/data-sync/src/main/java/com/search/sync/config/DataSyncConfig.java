package com.search.sync.config;

import com.search.sync.consumer.DataChangeConsumer;
import com.search.sync.processor.DataProcessor;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

/**
 * Configuration for data sync components
 *
 * Configures Kafka producer/consumer and wires up the data sync pipeline.
 */
@Configuration
public class DataSyncConfig {

    private static final Logger log = LoggerFactory.getLogger(DataSyncConfig.class);

    @Value("${kafka.bootstrap.servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${kafka.producer.topic:data-change-events}")
    private String producerTopic;

    @Value("${kafka.consumer.group.id:data-sync-group}")
    private String consumerGroupId;

    /**
     * Create Kafka producer for CDC events
     *
     * @return KafkaProducer
     */
    @Bean
    public KafkaProducer<String, String> kafkaProducer() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, "1");
        props.put(ProducerConfig.RETRIES_CONFIG, "3");

        log.info("Created Kafka producer for bootstrap servers: {}", bootstrapServers);
        return new KafkaProducer<>(props);
    }

    /**
     * Create data change consumer
     *
     * @param processor the data processor
     * @return DataChangeConsumer
     */
    @Bean
    public DataChangeConsumer dataChangeConsumer(DataProcessor processor) {
        String topic = "data-change-events";
        DataChangeConsumer consumer = new DataChangeConsumer(
                bootstrapServers,
                topic,
                consumerGroupId,
                processor
        );

        // Auto-start the consumer
        consumer.start();

        log.info("Created and started data change consumer for topic: {}", topic);
        return consumer;
    }
}
