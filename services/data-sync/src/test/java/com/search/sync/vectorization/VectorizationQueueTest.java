package com.search.sync.vectorization;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for VectorizationQueue
 */
class VectorizationQueueTest {

    private VectorizationQueue queue;

    @BeforeEach
    void setUp() {
        queue = new VectorizationQueue();
    }

    private VectorizationTask createTestTask(String id) {
        Map<String, Object> data = new HashMap<>();
        data.put("title", "Test " + id);
        data.put("description", "Description " + id);

        return new VectorizationTask(
                "test_index",
                id,
                List.of("title", "description"),
                "combined_vector",
                data
        );
    }

    @Test
    void testNewQueueIsEmpty() {
        assertTrue(queue.isEmpty());
        assertEquals(0, queue.size());
    }

    @Test
    void testEnqueueSingleTask() {
        VectorizationTask task = createTestTask("1");
        boolean added = queue.enqueue(task);

        assertTrue(added);
        assertEquals(1, queue.size());
        assertFalse(queue.isEmpty());
        assertEquals(1, queue.getEnqueuedCount());
        assertEquals(0, queue.getDroppedCount());
    }

    @Test
    void testEnqueueMultipleTasks() {
        for (int i = 0; i < 5; i++) {
            queue.enqueue(createTestTask(String.valueOf(i)));
        }

        assertEquals(5, queue.size());
        assertEquals(5, queue.getEnqueuedCount());
    }

    @Test
    void testDequeueTask() throws InterruptedException {
        VectorizationTask task = createTestTask("1");
        queue.enqueue(task);

        VectorizationTask dequeued = queue.dequeue();

        assertNotNull(dequeued);
        assertEquals("1", dequeued.getDocumentId());
        assertEquals(0, queue.size());
    }

    @Test
    void testDequeueFifoOrder() throws InterruptedException {
        queue.enqueue(createTestTask("1"));
        queue.enqueue(createTestTask("2"));
        queue.enqueue(createTestTask("3"));

        assertEquals("1", queue.dequeue().getDocumentId());
        assertEquals("2", queue.dequeue().getDocumentId());
        assertEquals("3", queue.dequeue().getDocumentId());
    }

    @Test
    void testPollWithTimeout() throws InterruptedException {
        // Poll from empty queue with short timeout
        VectorizationTask result = queue.poll(100);
        assertNull(result);

        // Add task and poll again
        queue.enqueue(createTestTask("1"));
        result = queue.poll(100);
        assertNotNull(result);
        assertEquals("1", result.getDocumentId());
    }

    @Test
    void testMarkProcessed() {
        queue.enqueue(createTestTask("1"));
        queue.enqueue(createTestTask("2"));

        queue.markProcessed();
        queue.markProcessed();

        assertEquals(2, queue.getProcessedCount());
        assertEquals(2, queue.getEnqueuedCount());
    }

    @Test
    void testGetStats() throws InterruptedException {
        queue.enqueue(createTestTask("1"));
        queue.enqueue(createTestTask("2"));

        // Poll removes the item from the queue
        queue.poll(0);
        queue.markProcessed();

        String stats = queue.getStats();

        assertTrue(stats.contains("size=1"));
        assertTrue(stats.contains("enqueued=2"));
        assertTrue(stats.contains("processed=1"));
        assertTrue(stats.contains("dropped=0"));
    }

    @Test
    void testDequeueBlocksWhenEmpty() throws InterruptedException {
        // This test verifies that dequeue blocks when queue is empty
        // We use a separate thread to add a task after a delay
        Thread enqueuer = new Thread(() -> {
            try {
                Thread.sleep(100);
                queue.enqueue(createTestTask("delayed"));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        enqueuer.start();

        long startTime = System.currentTimeMillis();
        VectorizationTask task = queue.dequeue();
        long elapsed = System.currentTimeMillis() - startTime;

        assertNotNull(task);
        assertEquals("delayed", task.getDocumentId());
        assertTrue(elapsed >= 100, "Should have blocked for at least 100ms");
        assertTrue(elapsed < 500, "Should not have blocked for more than 500ms");
    }

    @Test
    void testEnqueueAfterDequeue() throws InterruptedException {
        queue.enqueue(createTestTask("1"));
        queue.dequeue();

        assertEquals(0, queue.size());

        queue.enqueue(createTestTask("2"));
        assertEquals(1, queue.size());
        assertEquals("2", queue.dequeue().getDocumentId());
    }

    @Test
    void testMultipleMarkProcessedCalls() {
        queue.enqueue(createTestTask("1"));

        for (int i = 0; i < 5; i++) {
            queue.markProcessed();
        }

        assertEquals(5, queue.getProcessedCount());
    }

    @Test
    void testCountersAreAtomic() throws InterruptedException {
        // Test that counters work correctly with concurrent access
        Thread[] threads = new Thread[10];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 100; j++) {
                    queue.enqueue(createTestTask("thread-" + Thread.currentThread().getId() + "-" + j));
                    queue.markProcessed();
                }
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        assertEquals(1000, queue.getEnqueuedCount());
        assertEquals(1000, queue.getProcessedCount());
        assertEquals(1000, queue.size());
    }

    @Test
    void testGetEnqueuedCount() {
        assertEquals(0, queue.getEnqueuedCount());

        queue.enqueue(createTestTask("1"));
        assertEquals(1, queue.getEnqueuedCount());

        queue.enqueue(createTestTask("2"));
        assertEquals(2, queue.getEnqueuedCount());
    }

    @Test
    void testGetDroppedCount() {
        assertEquals(0, queue.getDroppedCount());
        // Note: Testing actual dropped count would require filling the 10000 capacity queue
        // which is impractical in unit tests. The counter logic is simple increment.
    }

    @Test
    void testGetProcessedCount() {
        assertEquals(0, queue.getProcessedCount());

        queue.markProcessed();
        assertEquals(1, queue.getProcessedCount());

        queue.markProcessed();
        queue.markProcessed();
        assertEquals(3, queue.getProcessedCount());
    }

    @Test
    void testPollWithTimeUnit() throws InterruptedException {
        queue.enqueue(createTestTask("1"));

        // poll with timeout
        VectorizationTask task = queue.poll(1000L, TimeUnit.MILLISECONDS);

        assertNotNull(task);
        assertEquals("1", task.getDocumentId());
    }

    @Test
    void testPollReturnsNullOnTimeout() throws InterruptedException {
        VectorizationTask task = queue.poll(10L, TimeUnit.MILLISECONDS);
        assertNull(task);
    }

    @Test
    void testEnqueueNullTask() {
        // The queue uses LinkedBlockingQueue which allows null?
        // Actually, BlockingQueue does not allow null, so this should throw
        assertThrows(NullPointerException.class, () -> queue.enqueue(null));
    }
}
