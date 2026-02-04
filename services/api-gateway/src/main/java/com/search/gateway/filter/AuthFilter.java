package com.search.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Authentication filter for API gateway
 *
 * Validates app credentials from request headers.
 * In production, this would integrate with a proper auth service.
 */
@Component
public class AuthFilter implements GatewayFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(AuthFilter.class);

    private static final String APP_KEY_HEADER = "X-App-Key";
    private static final String APP_SECRET_HEADER = "X-App-Secret";
    private static final String AUTH_TOKEN_HEADER = "Authorization";

    // In-memory credentials store (use database/Redis in production)
    private final Map<String, String> credentials;

    public AuthFilter() {
        this.credentials = Map.of(
                "app1", "secret1",
                "app2", "secret2",
                "test_app", "test_secret"
        );
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        // Skip auth for health check and public endpoints
        String path = request.getURI().getPath();
        if (path.equals("/health") || path.equals("/actuator/health")) {
            return chain.filter(exchange);
        }

        // Check for app credentials
        String appKey = request.getHeaders().getFirst(APP_KEY_HEADER);
        String appSecret = request.getHeaders().getFirst(APP_SECRET_HEADER);

        // Also allow Bearer token
        String authToken = request.getHeaders().getFirst(AUTH_TOKEN_HEADER);

        boolean authenticated = false;

        if (appKey != null && appSecret != null) {
            authenticated = validateAppCredentials(appKey, appSecret);
        } else if (authToken != null && authToken.startsWith("Bearer ")) {
            authenticated = validateAuthToken(authToken.substring(7));
        }

        if (!authenticated) {
            log.warn("Authentication failed: path={}, appKey={}", path, appKey);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        // Add app key to headers for downstream services
        ServerHttpRequest mutatedRequest = request.mutate()
                .header("X-Authenticated-App", appKey != null ? appKey : "token")
                .build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    /**
     * Validate app key and secret
     */
    private boolean validateAppCredentials(String appKey, String appSecret) {
        String expectedSecret = credentials.get(appKey);
        if (expectedSecret == null) {
            return false;
        }
        return expectedSecret.equals(appSecret);
    }

    /**
     * Validate Bearer token
     *
     * In production, this would validate JWT tokens
     */
    private boolean validateAuthToken(String token) {
        // Simple validation for development
        // In production, use proper JWT validation
        return token != null && !token.isEmpty() && token.length() > 10;
    }

    @Override
    public int getOrder() {
        return -100; // Run early in the filter chain
    }
}
