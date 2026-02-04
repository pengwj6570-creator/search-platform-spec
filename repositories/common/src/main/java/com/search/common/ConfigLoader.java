package com.search.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * YAML configuration loader for search platform.
 * Loads configuration from YAML file and provides cached access.
 * Thread-safe for concurrent access.
 */
public class ConfigLoader {

    private static final Logger logger = LoggerFactory.getLogger(ConfigLoader.class);

    private static final ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());

    private static final AtomicReference<Config> cachedConfig = new AtomicReference<>();

    private ConfigLoader() {
        // Utility class - prevent instantiation
    }

    /**
     * Load configuration from the specified YAML file path.
     * Thread-safe - can be called from multiple threads.
     *
     * @param path the path to the YAML configuration file
     * @throws RuntimeException if the file cannot be read or parsed
     */
    public static synchronized void load(String path) {
        try {
            logger.info("Loading configuration from: {}", path);
            Path filePath = Paths.get(path);

            if (!Files.exists(filePath)) {
                throw new RuntimeException("Configuration file not found: " + path);
            }

            Config config = objectMapper.readValue(filePath.toFile(), Config.class);
            cachedConfig.set(config);
            logger.info("Configuration loaded successfully");
        } catch (IOException e) {
            throw new RuntimeException("Failed to load configuration from: " + path, e);
        }
    }

    /**
     * Load configuration from a classpath resource.
     * Thread-safe - can be called from multiple threads.
     *
     * @param resourceName the name of the resource on the classpath
     * @throws RuntimeException if the resource cannot be read or parsed
     */
    public static synchronized void loadFromClasspath(String resourceName) {
        try (InputStream is = ConfigLoader.class.getClassLoader().getResourceAsStream(resourceName)) {
            if (is == null) {
                throw new RuntimeException("Configuration resource not found on classpath: " + resourceName);
            }
            Config config = objectMapper.readValue(is, Config.class);
            cachedConfig.set(config);
            logger.info("Configuration loaded successfully from classpath: {}", resourceName);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load configuration from classpath: " + resourceName, e);
        }
    }

    /**
     * Get the cached configuration.
     * Thread-safe - returns immutable snapshot.
     *
     * @return the loaded configuration
     * @throws IllegalStateException if configuration has not been loaded
     */
    public static Config get() {
        Config config = cachedConfig.get();
        if (config == null) {
            throw new IllegalStateException("Configuration has not been loaded. Call load() first.");
        }
        return config;
    }

    /**
     * Get the underlying ObjectMapper for custom operations.
     *
     * @return the static ObjectMapper instance
     */
    public static ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    /**
     * Reset the cached configuration (primarily for testing).
     */
    static void reset() {
        cachedConfig.set(null);
    }

    /**
     * Main configuration class.
     */
    public static class Config {
        private Elasticsearch elasticsearch;
        private Kafka kafka;

        public Elasticsearch getElasticsearch() {
            return elasticsearch;
        }

        public void setElasticsearch(Elasticsearch elasticsearch) {
            this.elasticsearch = elasticsearch;
        }

        public Kafka getKafka() {
            return kafka;
        }

        public void setKafka(Kafka kafka) {
            this.kafka = kafka;
        }
    }

    /**
     * Elasticsearch configuration.
     */
    public static class Elasticsearch {
        private List<String> hosts = new ArrayList<>();
        private String username;
        private String password;
        private String indexPrefix;

        public List<String> getHosts() {
            return hosts;
        }

        public void setHosts(List<String> hosts) {
            this.hosts = hosts;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getIndexPrefix() {
            return indexPrefix;
        }

        public void setIndexPrefix(String indexPrefix) {
            this.indexPrefix = indexPrefix;
        }
    }

    /**
     * Kafka configuration.
     */
    public static class Kafka {
        private String bootstrapServers;
        private String consumerGroup;
        private List<String> topics = new ArrayList<>();

        public String getBootstrapServers() {
            return bootstrapServers;
        }

        public void setBootstrapServers(String bootstrapServers) {
            this.bootstrapServers = bootstrapServers;
        }

        public String getConsumerGroup() {
            return consumerGroup;
        }

        public void setConsumerGroup(String consumerGroup) {
            this.consumerGroup = consumerGroup;
        }

        public List<String> getTopics() {
            return topics;
        }

        public void setTopics(List<String> topics) {
            this.topics = topics;
        }
    }
}
