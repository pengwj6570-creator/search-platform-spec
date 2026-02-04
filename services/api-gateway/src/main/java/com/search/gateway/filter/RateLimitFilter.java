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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Rate limiting filter for API gateway
 *
 * Implements token bucket rate limiting per app.
 * In production, use Redis or a dedicated rate limiting service.
 */
@Component
public class RateLimitFilter implements GatewayFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    // Default QPS limits per app (use database/Redis in production)
    private static final int DEFAULT_QPS = 100;
    private static final Map<String, Integer> APP_LIMITS = Map.of(
            "app1", 100,
            "app2", 50,
            "test_app", 10
    );

    // In-memory rate limiters (use Redis in production)
    private final ConcurrentHashMap<String, RateLimiter> limiters = new ConcurrentHashMap<>();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // Skip rate limiting for health check
        if (path.equals("/health") || path.equals("/actuator/health")) {
            return chain.filter(exchange);
        }

        // Get app key from headers (set by AuthFilter)
        String appKey = request.getHeaders().getFirst("X-Authenticated-App");
        if (appKey == null) {
            appKey = request.getHeaders().getFirst("X-App-Key");
        }
        if (appKey == null) {
            appKey = "anonymous";
        }

        // Get or create rate limiter for this app
        int qps = APP_LIMITS.getOrDefault(appKey, DEFAULT_QPS);
        RateLimiter limiter = limiters.computeIfAbsent(appKey, k -> new RateLimiter(qps));

        // Try to acquire a token
        if (!limiter.tryAcquire()) {
            log.warn("Rate limit exceeded: appKey={}, qps={}", appKey, qps);
            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            exchange.getResponse().getHeaders().add("X-RateLimit-Limit", String.valueOf(qps));
            exchange.getResponse().getHeaders().add("X-RateLimit-Remaining", "0");
            return exchange.getResponse().setComplete();
        }

        // Add rate limit headers
        long remaining = limiter.getRemainingTokens();
        exchange.getResponse().getHeaders().add("X-RateLimit-Limit", String.valueOf(qps));
        exchange.getResponse().getHeaders().add("X-RateLimit-Remaining", String.valueOf(remaining));

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -99; // Run after AuthFilter
    }

    /**
     * Simple token bucket rate limiter
     */
    static class RateLimiter {
        private final int qps;
        private final long windowSizeMs = 1000; // 1 second window
        private volatile long windowStart;
        private final AtomicLong counter;

        RateLimiter(int qps) {
            this.qps = qps;
            this.windowStart = System.currentTimeMillis();
            this.counter = new AtomicLong(0);
        }

        boolean tryAcquire() {
            long now = System.currentTimeMillis();

            // Reset window if expired
            if (now - windowStart >= windowSizeMs) {
                synchronized (this) {
                    if (now - windowStart >= windowSizeMs) {
                        windowStart = now;
                        counter.set(0);
                    }
                }
            }

            // Try to increment counter
            long current = counter.incrementAndGet();
            if (current > qps) {
                counter.decrementAndGet();
                return false;
            }
            return true;
        }

        long getRemainingTokens() {
            long current = counter.get();
            return Math.max(0, qps - current);
        }
    }
}
