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
 */
@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // Health check route
                .route("health", r -> r
                        .path("/health", "/actuator/health")
                        .filters(f -> f
                                .stripPrefix(0)
                        )
                        .uri("lb://health")
                )

                // Config Admin routes
                .route("config-admin", r -> r
                        .path("/api/v1/sources/**", "/api/v1/objects/**")
                        .filters(f -> f
                                .stripPrefix(0)
                                .filter(new AuthFilter())
                                .filter(new RateLimitFilter())
                        )
                        .uri("lb://config-admin")
                )

                // Query Service routes
                .route("query-service", r -> r
                        .path("/api/v1/search/**")
                        .filters(f -> f
                                .stripPrefix(0)
                                .filter(new AuthFilter())
                                .filter(new RateLimitFilter())
                        )
                        .uri("lb://query-service")
                )

                // Vector Service routes
                .route("vector-service", r -> r
                        .path("/api/v1/embedding/**")
                        .filters(f -> f
                                .stripPrefix(0)
                                .filter(new AuthFilter())
                                .filter(new RateLimitFilter())
                        )
                        .uri("lb://vector-service")
                )

                // Data Sync routes (internal, may need special auth)
                .route("data-sync", r -> r
                        .path("/api/v1/sync/**")
                        .filters(f -> f
                                .stripPrefix(0)
                                .filter(new AuthFilter())
                        )
                        .uri("lb://data-sync")
                )

                .build();
    }
}
