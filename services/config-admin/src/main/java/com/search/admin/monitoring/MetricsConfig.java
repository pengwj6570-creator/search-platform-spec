package com.search.admin.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Metrics configuration for config-admin service
 */
@Configuration
public class MetricsConfig {

    @Autowired
    public void configureMetrics(MeterRegistry registry) {
        // Custom counters can be registered here
        Counter.builder("config.admin.requests")
                .description("Total number of requests to config admin")
                .register(registry);

        // Gauges for tracking current state
        registry.gauge("config.admin.sources.count", new AtomicLong(0),
                "Current number of sources");

        registry.gauge("config.admin.objects.count", new AtomicLong(0),
                "Current number of search objects");
    }
}
