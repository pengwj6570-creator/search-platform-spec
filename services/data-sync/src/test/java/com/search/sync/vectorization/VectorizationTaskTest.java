package com.search.sync.vectorization;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for VectorizationTask
 */
class VectorizationTaskTest {

    private Map<String, Object> documentData;
    private VectorizationTask task;

    @BeforeEach
    void setUp() {
        documentData = new HashMap<>();
        documentData.put("title", "Test Product");
        documentData.put("description", "A great product for testing");
        documentData.put("price", 99.99);
        documentData.put("id", "12345");

        task = new VectorizationTask(
                "test_index",
                "doc123",
                List.of("title", "description"),
                "combined_vector",
                documentData
        );
    }

    @Test
    void testTaskCreation() {
        assertEquals("test_index", task.getIndexName());
        assertEquals("doc123", task.getDocumentId());
        assertEquals("combined_vector", task.getTargetField());
        assertEquals(2, task.getSourceFields().size());
        assertEquals("title", task.getSourceFields().get(0));
        assertEquals("description", task.getSourceFields().get(1));
        assertNotNull(task.getDocumentData());
        assertEquals("Test Product", task.getDocumentData().get("title"));
    }

    @Test
    void testCombineTextWithMultipleFields() {
        String combined = task.combineText();
        assertEquals("Test Product A great product for testing", combined);
    }

    @Test
    void testCombineTextWithSingleField() {
        VectorizationTask singleFieldTask = new VectorizationTask(
                "test_index",
                "doc123",
                List.of("title"),
                "title_vector",
                documentData
        );

        String combined = singleFieldTask.combineText();
        assertEquals("Test Product", combined);
    }

    @Test
    void testCombineTextWithNonExistentField() {
        VectorizationTask taskWithMissingField = new VectorizationTask(
                "test_index",
                "doc123",
                List.of("title", "nonexistent"),
                "combined_vector",
                documentData
        );

        String combined = taskWithMissingField.combineText();
        assertEquals("Test Product", combined); // Only title, nonexistent is ignored
    }

    @Test
    void testCombineTextWithEmptySourceFields() {
        VectorizationTask emptyTask = new VectorizationTask(
                "test_index",
                "doc123",
                null,
                "vector",
                documentData
        );

        String combined = emptyTask.combineText();
        assertEquals("", combined);
    }

    @Test
    void testCombineTextWithNullFieldValue() {
        documentData.put("empty_field", null);
        VectorizationTask taskWithNull = new VectorizationTask(
                "test_index",
                "doc123",
                List.of("title", "empty_field"),
                "combined_vector",
                documentData
        );

        String combined = taskWithNull.combineText();
        assertEquals("Test Product", combined); // null value is ignored
    }

    @Test
    void testRetryLogic() {
        assertEquals(0, task.getRetryCount());
        assertTrue(task.shouldRetry());

        task.incrementRetry();
        assertEquals(1, task.getRetryCount());
        assertTrue(task.shouldRetry());

        task.incrementRetry();
        assertEquals(2, task.getRetryCount());
        assertTrue(task.shouldRetry());

        task.incrementRetry();
        assertEquals(3, task.getRetryCount());
        assertFalse(task.shouldRetry()); // MAX_RETRIES is 3
    }

    @Test
    void testCreatedAtTimestamp() {
        long beforeCreate = System.currentTimeMillis();
        VectorizationTask newTask = new VectorizationTask(
                "test_index",
                "doc123",
                List.of("title"),
                "vector",
                documentData
        );
        long afterCreate = System.currentTimeMillis();

        assertTrue(newTask.getCreatedAt() >= beforeCreate);
        assertTrue(newTask.getCreatedAt() <= afterCreate);
    }

    @Test
    void testToString() {
        String str = task.toString();
        assertTrue(str.contains("test_index"));
        assertTrue(str.contains("doc123"));
        assertTrue(str.contains("combined_vector"));
        assertTrue(str.contains("retryCount=0"));
    }

    @Test
    void testCombineTextWithNumericField() {
        VectorizationTask numericTask = new VectorizationTask(
                "test_index",
                "doc123",
                List.of("price"),
                "price_vector",
                documentData
        );

        String combined = numericTask.combineText();
        assertEquals("99.99", combined);
    }

    @Test
    void testMultipleIncrementsBeyondMaxRetries() {
        for (int i = 0; i < 10; i++) {
            task.incrementRetry();
        }
        assertEquals(10, task.getRetryCount());
        assertFalse(task.shouldRetry());
    }
}
