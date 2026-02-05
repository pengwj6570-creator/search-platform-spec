package com.search.sync.config;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
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
     * RestTemplate for HTTP requests to vectorization service
     *
     * @return RestTemplate
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    /**
     * Create Kafka producer for CDC events
     *
     * @return KafkaProducer
     */
    @Bean
    public KafkaProducer<String, String> kafkaProducer() {
        Properties props = new Properties();
        props.put("bootstrap.servers", bootstrapServers);
        props.put("key.serializer", StringSerializer.class.getName());
        props.put("value.serializer", StringSerializer.class.getName());
        props.put("acks", "1");
        props.put("retries", "3");

        log.info("Created Kafka producer for bootstrap servers: {}", bootstrapServers);
        return new KafkaProducer<>(props);
    }
}
