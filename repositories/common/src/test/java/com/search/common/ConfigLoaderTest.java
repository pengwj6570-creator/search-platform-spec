package com.search.common;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ConfigLoaderTest {

    @TempDir
    Path tempDir;

    private String testConfigPath;

    @BeforeEach
    void setUp() throws IOException {
        Path configFile = tempDir.resolve("config.yaml");
        testConfigPath = configFile.toString();

        String yamlContent = """
                elasticsearch:
                  hosts:
                    - http://localhost:9200
                  username: elastic
                  password: changeme
                  indexPrefix: search_
                kafka:
                  bootstrapServers: localhost:9092
                  consumerGroup: search-consumer
                  topics:
                    - search-events
                    - index-updates
                """;
        Files.writeString(configFile, yamlContent);
    }

    @Test
    void testLoadConfig() {
        ConfigLoader.load(testConfigPath);

        ConfigLoader.Config config = ConfigLoader.get();

        assertNotNull(config, "Config should not be null");
        assertNotNull(config.getElasticsearch(), "Elasticsearch config should not be null");
        assertNotNull(config.getKafka(), "Kafka config should not be null");
    }

    @Test
    void testElasticsearchConfig() {
        ConfigLoader.load(testConfigPath);

        ConfigLoader.Config config = ConfigLoader.get();
        ConfigLoader.Elasticsearch esConfig = config.getElasticsearch();

        assertNotNull(esConfig, "Elasticsearch config should not be null");
        assertNotNull(esConfig.getHosts(), "Hosts should not be null");
        assertEquals(1, esConfig.getHosts().size(), "Should have one host");
        assertEquals("http://localhost:9200", esConfig.getHosts().get(0), "Host should match");
        assertEquals("elastic", esConfig.getUsername(), "Username should match");
        assertEquals("changeme", esConfig.getPassword(), "Password should match");
        assertEquals("search_", esConfig.getIndexPrefix(), "Index prefix should match");
    }

    @Test
    void testKafkaConfig() {
        ConfigLoader.load(testConfigPath);

        ConfigLoader.Config config = ConfigLoader.get();
        ConfigLoader.Kafka kafkaConfig = config.getKafka();

        assertNotNull(kafkaConfig, "Kafka config should not be null");
        assertEquals("localhost:9092", kafkaConfig.getBootstrapServers(), "Bootstrap servers should match");
        assertEquals("search-consumer", kafkaConfig.getConsumerGroup(), "Consumer group should match");
        assertNotNull(kafkaConfig.getTopics(), "Topics should not be null");
        assertEquals(2, kafkaConfig.getTopics().size(), "Should have two topics");
        assertEquals("search-events", kafkaConfig.getTopics().get(0), "First topic should match");
        assertEquals("index-updates", kafkaConfig.getTopics().get(1), "Second topic should match");
    }

    @Test
    void testLoadInvalidPath() {
        assertThrows(RuntimeException.class, () -> ConfigLoader.load("/nonexistent/path/config.yaml"),
                "Should throw RuntimeException for invalid path");
    }

    @Test
    void testGetBeforeLoad() {
        assertThrows(IllegalStateException.class, ConfigLoader::get,
                "Should throw IllegalStateException when get is called before load");
    }
}
