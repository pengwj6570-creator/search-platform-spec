package com.search.sync;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Data Synchronization Application
 *
 * Consumes CDC events from Debezium and synchronizes data to OpenSearch.
 * Supports MySQL, PostgreSQL, and Oracle data sources.
 *
 * Supports asynchronous vectorization via the vectorization queue.
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class DataSyncApplication {

    public static void main(String[] args) {
        SpringApplication.run(DataSyncApplication.class, args);
    }
}
