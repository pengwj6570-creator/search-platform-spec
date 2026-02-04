package com.search.sync.vectorization;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory queue for vectorization tasks
 *
 * In production, this should be replaced with a persistent queue
 * like Kafka or Redis for durability and scaling.
 */
@Component
public class VectorizationQueue {

    private static final Logger log = LoggerFactory.getLogger(VectorizationQueue.class);

    /**
     * Queue capacity - prevents unbounded memory growth
     * When full, oldest tasks are dropped
     */
    private static final int QUEUE_CAPACITY = 10000;

    private final BlockingQueue<VectorizationTask> queue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);

    /**
     * Counter for tracking statistics
     */
    private final AtomicLong enqueuedCount = new AtomicLong(0);
    private final AtomicLong droppedCount = new AtomicLong(0);
    private final AtomicLong processedCount = new AtomicLong(0);

    /**
     * Add a task to the queue
     *
     * @param task the vectorization task
     * @return true if added successfully, false if queue is full
     */
    public boolean enqueue(VectorizationTask task) {
        boolean added = queue.offer(task);
        if (added) {
            enqueuedCount.incrementAndGet();
            log.debug("Enqueued vectorization task: {}", task);
        } else {
            droppedCount.incrementAndGet();
            log.warn("Vectorization queue full, dropping task: {}", task);
        }
        return added;
    }

    /**
     * Take a task from the queue, blocking if empty
     *
     * @return the next vectorization task
     * @throws InterruptedException if interrupted while waiting
     */
    public VectorizationTask dequeue() throws InterruptedException {
        VectorizationTask task = queue.take();
        return task;
    }

    /**
     * Poll for a task with timeout
     *
     * @param timeoutMs timeout in milliseconds
     * @return the task, or null if timeout
     * @throws InterruptedException if interrupted while waiting
     */
    public VectorizationTask poll(long timeoutMs) throws InterruptedException {
        return queue.poll(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    /**
     * Get current queue size
     *
     * @return queue size
     */
    public int size() {
        return queue.size();
    }

    /**
     * Check if queue is empty
     *
     * @return true if empty
     */
    public boolean isEmpty() {
        return queue.isEmpty();
    }

    /**
     * Get statistics about the queue
     *
     * @return statistics string
     */
    public String getStats() {
        return String.format("VectorizationQueue[size=%d, enqueued=%d, processed=%d, dropped=%d]",
                queue.size(), enqueuedCount.get(), processedCount.get(), droppedCount.get());
    }

    /**
     * Mark a task as processed
     */
    public void markProcessed() {
        processedCount.incrementAndGet();
    }

    /**
     * Get the number of enqueued tasks
     */
    public long getEnqueuedCount() {
        return enqueuedCount.get();
    }

    /**
     * Get the number of dropped tasks
     */
    public long getDroppedCount() {
        return droppedCount.get();
    }

    /**
     * Get the number of processed tasks
     */
    public long getProcessedCount() {
        return processedCount.get();
    }
}
