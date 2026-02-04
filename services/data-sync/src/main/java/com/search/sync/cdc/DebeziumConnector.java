package com.search.sync.cdc;

import io.debezium.engine.DebeziumEngine;
import io.debezium.engine.format.Json;
import io.debezium.config.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Debezium CDC connector for capturing data changes from source databases
 *
 * Supports MySQL, PostgreSQL, and Oracle as source systems.
 * Changes are published to Kafka for downstream processing.
 */
@Component
public class DebeziumConnector {

    private static final Logger log = LoggerFactory.getLogger(DebeziumConnector.class);

    private final AtomicBoolean running = new AtomicBoolean(false);
    private DebeziumEngine<?> engine;
    private ExecutorService executor;

    /**
     * Start the Debezium connector with the given configuration
     *
     * @param config Debezium configuration
     * @param handler event handler for processing changes
     */
    public void start(Configuration config, Consumer<List<String>> handler) {
        if (running.getAndSet(true)) {
            log.warn("Debezium connector is already running");
            return;
        }

        try {
            // Create engine - using Debezium 2.x API
            engine = DebeziumEngine.create(Json.class)
                    .using(config.asProperties())
                    .notifying(handler)
                    .build();

            executor = Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "debezium-connector");
                t.setDaemon(false);
                return t;
            });

            executor.execute(engine);
            log.info("Debezium connector started successfully");
        } catch (Exception e) {
            log.error("Failed to start Debezium connector", e);
            running.set(false);
            throw new RuntimeException("Failed to start Debezium connector", e);
        }
    }

    /**
     * Stop the Debezium connector
     */
    public void stop() {
        if (!running.getAndSet(false)) {
            log.warn("Debezium connector is not running");
            return;
        }

        try {
            if (engine != null) {
                engine.close();
                log.info("Debezium engine closed");
            }
            if (executor != null) {
                executor.shutdown();
                log.info("Executor service shutdown");
            }
        } catch (Exception e) {
            log.error("Error stopping Debezium connector", e);
        }
    }

    /**
     * Check if the connector is running
     *
     * @return true if running, false otherwise
     */
    public boolean isRunning() {
        return running.get();
    }

    /**
     * Create Debezium configuration for MySQL
     *
     * @param name unique connector name
     * @param hostname database host
     * @param port database port
     * @param database database name
     * @param user database user
     * @param password database password
     * @return Debezium configuration
     */
    public static Configuration createMySqlConfig(String name, String hostname, int port,
                                                   String database, String user, String password) {
        return Configuration.create()
                .with("name", name)
                .with("connector.class", "io.debezium.connector.mysql.MySqlConnector")
                .with("database.hostname", hostname)
                .with("database.port", String.valueOf(port))
                .with("database.user", user)
                .with("database.password", password)
                .with("database.server.id", "184054")
                .with("database.server.name", "dbserver1")
                .with("database.include.list", database)
                .with("database.history.kafka.bootstrap.servers", "localhost:9092")
                .with("database.history.kafka.topic", "schema-changes." + name)
                .with("offset.storage", "org.apache.kafka.connect.storage.FileOffsetBackingStore")
                .with("offset.storage.file.filename", "/tmp/offsets_" + name + ".dat")
                .with("offset.flush.interval.ms", "6000")
                .build();
    }

    /**
     * Create Debezium configuration for PostgreSQL
     *
     * @param name unique connector name
     * @param hostname database host
     * @param port database port
     * @param database database name
     * @param user database user
     * @param password database password
     * @return Debezium configuration
     */
    public static Configuration createPostgresConfig(String name, String hostname, int port,
                                                      String database, String user, String password) {
        return Configuration.create()
                .with("name", name)
                .with("connector.class", "io.debezium.connector.postgresql.PostgresConnector")
                .with("database.hostname", hostname)
                .with("database.port", String.valueOf(port))
                .with("database.user", user)
                .with("database.password", password)
                .with("database.dbname", database)
                .with("database.server.name", "postgres_" + name)
                .with("slot.name", "debezium_" + name)
                .with("plugin.name", "pgoutput")
                .with("database.history.kafka.bootstrap.servers", "localhost:9092")
                .with("database.history.kafka.topic", "schema-changes." + name)
                .with("offset.storage", "org.apache.kafka.connect.storage.FileOffsetBackingStore")
                .with("offset.storage.file.filename", "/tmp/offsets_" + name + ".dat")
                .with("offset.flush.interval.ms", "6000")
                .build();
    }

    /**
     * Create Debezium configuration for Oracle
     *
     * @param name unique connector name
     * @param hostname database host
     * @param port database port
     * @param database database name (SID or service name)
     * @param user database user
     * @param password database password
     * @return Debezium configuration
     */
    public static Configuration createOracleConfig(String name, String hostname, int port,
                                                    String database, String user, String password) {
        return Configuration.create()
                .with("name", name)
                .with("connector.class", "io.debezium.connector.oracle.OracleConnector")
                .with("database.hostname", hostname)
                .with("database.port", String.valueOf(port))
                .with("database.user", user)
                .with("database.password", password)
                .with("database.dbname", database)
                .with("database.server.name", "oracle_" + name)
                .with("database.history.kafka.bootstrap.servers", "localhost:9092")
                .with("database.history.kafka.topic", "schema-changes." + name)
                .with("offset.storage", "org.apache.kafka.connect.storage.FileOffsetBackingStore")
                .with("offset.storage.file.filename", "/tmp/offsets_" + name + ".dat")
                .with("offset.flush.interval.ms", "6000")
                .build();
    }
}
