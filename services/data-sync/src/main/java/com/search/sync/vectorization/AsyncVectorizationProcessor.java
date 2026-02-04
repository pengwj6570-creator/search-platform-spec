package com.search.sync.vectorization;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;

/**
 * Asynchronous processor for vectorization tasks
 *
 * Runs in the background, processing tasks from the VectorizationQueue.
 * Bypass mode: documents are indexed first, then vectorized asynchronously.
 */
@Component
@ConditionalOnProperty(name = "vectorization.async.enabled", havingValue = "true", matchIfMissing = true)
public class AsyncVectorizationProcessor {

    private static final Logger log = LoggerFactory.getLogger(AsyncVectorizationProcessor.class);

    private final VectorizationQueue queue;
    private final VectorizationService vectorizationService;

    private volatile boolean running = true;

    @Autowired
    public AsyncVectorizationProcessor(VectorizationQueue queue,
                                       VectorizationService vectorizationService) {
        this.queue = queue;
        this.vectorizationService = vectorizationService;
    }

    /**
     * Process vectorization queue periodically
     * Runs every 100ms to check for new tasks
     */
    @Scheduled(fixedDelay = 100, initialDelay = 1000)
    public void processQueue() {
        if (!running) {
            return;
        }

        try {
            // Check if vector service is available
            if (!vectorizationService.isVectorServiceAvailable()) {
                // Service not available, skip this round
                return;
            }

            // Process available tasks (batch up to 10 at a time)
            int batchSize = 10;
            int processed = 0;

            while (processed < batchSize && !queue.isEmpty()) {
                VectorizationTask task = queue.poll(100);
                if (task == null) {
                    break;
                }

                boolean success = vectorizationService.processTask(task);
                if (success) {
                    queue.markProcessed();
                    processed++;
                } else {
                    // Retry logic
                    task.incrementRetry();
                    if (task.shouldRetry()) {
                        log.warn("Retrying vectorization task (attempt {}): {}",
                                task.getRetryCount(), task);
                        queue.enqueue(task);
                    } else {
                        log.error("Max retries exceeded for task: {}", task);
                    }
                }
            }

            if (processed > 0) {
                log.debug("Processed {} vectorization tasks", processed);
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Vectorization processor interrupted");
        } catch (Exception e) {
            log.error("Error in vectorization processor", e);
        }
    }

    /**
     * Log queue statistics every 30 seconds
     */
    @Scheduled(fixedRate = 30000)
    public void logStats() {
        if (!running) {
            return;
        }
        log.info(queue.getStats());
    }

    /**
     * Shutdown hook
     */
    @PreDestroy
    public void shutdown() {
        log.info("Shutting down vectorization processor...");
        running = false;
        log.info("Vectorization processor stopped. Final stats: {}", queue.getStats());
    }

    /**
     * Check if processor is running
     */
    public boolean isRunning() {
        return running;
    }
}
