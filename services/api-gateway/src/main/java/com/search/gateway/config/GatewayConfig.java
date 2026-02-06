package com.search.gateway.config;

import com.search.gateway.filter.AuthFilter;
import com.search.gateway.filter.RateLimitFilter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Gateway configuration
 *
 * Configures routes and filters for the API gateway.
 * Uses direct URLs for Docker container-to-container communication.
 */
@Configuration
public class GatewayConfig {

    // Service URLs - using Docker network aliases
    // In Docker Compose, services can reach each other by service name
    private static final String CONFIG_ADMIN_URL = "http://config-admin:8080";
    private static final String QUERY_SERVICE_URL = "http://query-service:8082";
    private static final String VECTOR_SERVICE_URL = "http://vector-service:8083";
    private static final String DATA_SYNC_URL = "http://data-sync:8081";

    // Alternative: Use host gateway for external access
    // Uncomment if running in development with services on host
    // private static final String CONFIG_ADMIN_URL = "http://host.docker.internal:8080";
    // private static final String QUERY_SERVICE_URL = "http://host.docker.internal:8082";
    // private static final String VECTOR_SERVICE_URL = "http://host.docker.internal:8083";

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // Health check route (no auth required)
                .route("health", r -> r
                        .path("/health", "/actuator/health")
                        .filters(f -> f
                                .stripPrefix(0)
                        )
                        .uri("http://localhost:8084")
                )

                // Config Admin routes
                .route("config-admin", r -> r
                        .path("/api/v1/sources/**", "/api/v1/objects/**")
                        .filters(f -> f
                                .stripPrefix(0)
                                .filter(new AuthFilter())
                                .filter(new RateLimitFilter())
                        )
                        .uri(CONFIG_ADMIN_URL)
                )

                // Query Service routes
                .route("query-service", r -> r
                        .path("/api/v1/search/**")
                        .filters(f -> f
                                .stripPrefix(0)
                                .filter(new AuthFilter())
                                .filter(new RateLimitFilter())
                        )
                        .uri(QUERY_SERVICE_URL)
                )

                // Vector Service routes
                .route("vector-service", r -> r
                        .path("/api/v1/embedding/**", "/api/v1/embedding")
                        .filters(f -> f
                                .stripPrefix(0)
                                .filter(new AuthFilter())
                                .filter(new RateLimitFilter())
                        )
                        .uri(VECTOR_SERVICE_URL)
                )

                // Data Sync routes (internal, may need special auth)
                .route("data-sync", r -> r
                        .path("/api/v1/sync/**")
                        .filters(f -> f
                                .stripPrefix(0)
                                .filter(new AuthFilter())
                        )
                        .uri(DATA_SYNC_URL)
                )

                .build();
    }
}
