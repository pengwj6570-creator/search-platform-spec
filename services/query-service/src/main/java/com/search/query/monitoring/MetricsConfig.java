package com.search.query.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

/**
 * Metrics configuration for query-service
 */
@Configuration
public class MetricsConfig {

    @Autowired
    public void configureMetrics(MeterRegistry registry) {
        // Search request counter
        Counter.builder("search.requests.total")
                .description("Total number of search requests")
                .register(registry);

        // Search request timer
        Timer.builder("search.requests.duration")
                .description("Search request duration")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);

        // Recall metrics
        Counter.builder("search.recall.keyword.total")
                .description("Total number of keyword recalls")
                .register(registry);

        Counter.builder("search.recall.vector.total")
                .description("Total number of vector recalls")
                .register(registry);

        Counter.builder("search.recall.hot.total")
                .description("Total number of hot recalls")
                .register(registry);

        // Rerank counter
        Counter.builder("search.rerank.total")
                .description("Total number of rerank operations")
                .register(registry);
    }
}
