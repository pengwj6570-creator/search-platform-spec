package com.search.sync.processor;

import com.search.sync.vectorization.VectorizationQueue;
import com.search.sync.vectorization.VectorizationTask;
import com.search.sync.writer.ESWriter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DataProcessor
 */
class DataProcessorTest {

    @Mock
    private ESWriter mockEsWriter;

    @Mock
    private VectorizationQueue mockVectorizationQueue;

    private DataProcessor dataProcessor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        dataProcessor = new DataProcessor(mockEsWriter);
        ReflectionTestUtils.setField(dataProcessor, "vectorizationQueue", mockVectorizationQueue);
        ReflectionTestUtils.setField(dataProcessor, "vectorizationEnabled", true);
    }

    @AfterEach
    void tearDown() {
        // Cleanup handled by GC
    }

    @Test
    void testProcessCreateEvent() {
        String cdcEvent = """
                {
                    "op": "c",
                    "source": {
                        "table": "products"
                    },
                    "after": {
                        "id": "123",
                        "title": "Test Product",
                        "price": 99.99
                    }
                }
                """;

        dataProcessor.process(cdcEvent);

        // Verify ESWriter.upsert was called
        verify(mockEsWriter).upsert(eq("products"), eq("123"), contains("Test Product"));

        // Verify vectorization task was enqueued (title + description heuristic doesn't match)
        // But content field heuristic also doesn't match, so no task should be enqueued
        verify(mockVectorizationQueue, never()).enqueue(any());
    }

    @Test
    void testProcessUpdateEvent() {
        String cdcEvent = """
                {
                    "op": "u",
                    "source": {
                        "table": "products"
                    },
                    "after": {
                        "id": "456",
                        "title": "Updated Product",
                        "description": "Updated description"
                    }
                }
                """;

        dataProcessor.process(cdcEvent);

        verify(mockEsWriter).upsert(eq("products"), eq("456"), contains("Updated Product"));
    }

    @Test
    void testProcessDeleteEvent() {
        String cdcEvent = """
                {
                    "op": "d",
                    "source": {
                        "table": "products"
                    },
                    "before": {
                        "id": "789",
                        "title": "Deleted Product"
                    }
                }
                """;

        dataProcessor.process(cdcEvent);

        verify(mockEsWriter).delete(eq("products"), eq("789"));
        verify(mockVectorizationQueue, never()).enqueue(any());
    }

    @Test
    void testProcessReadSnapshotEvent() {
        String cdcEvent = """
                {
                    "op": "r",
                    "source": {
                        "table": "products"
                    },
                    "after": {
                        "id": "101",
                        "name": "Snapshot Product"
                    }
                }
                """;

        dataProcessor.process(cdcEvent);

        verify(mockEsWriter).upsert(eq("products"), eq("101"), contains("Snapshot Product"));
    }

    @Test
    void testProcessEventWithMissingOp() {
        String cdcEvent = """
                {
                    "source": {
                        "table": "products"
                    },
                    "after": {
                        "id": "123"
                    }
                }
                """;

        dataProcessor.process(cdcEvent);

        // Should not call any methods
        verify(mockEsWriter, never()).upsert(any(), any(), any());
        verify(mockEsWriter, never()).delete(any(), any());
    }

    @Test
    void testProcessEventWithMissingSource() {
        String cdcEvent = """
                {
                    "op": "c",
                    "after": {
                        "id": "123"
                    }
                }
                """;

        dataProcessor.process(cdcEvent);

        verify(mockEsWriter, never()).upsert(any(), any(), any());
    }

    @Test
    void testProcessEventWithMissingTable() {
        String cdcEvent = """
                {
                    "op": "c",
                    "source": {},
                    "after": {
                        "id": "123"
                    }
                }
                """;

        dataProcessor.process(cdcEvent);

        verify(mockEsWriter, never()).upsert(any(), any(), any());
    }

    @Test
    void testProcessEventWithMissingAfterNodeForCreate() {
        String cdcEvent = """
                {
                    "op": "c",
                    "source": {
                        "table": "products"
                    }
                }
                """;

        dataProcessor.process(cdcEvent);

        verify(mockEsWriter, never()).upsert(any(), any(), any());
    }

    @Test
    void testProcessEventWithMissingBeforeNodeForDelete() {
        String cdcEvent = """
                {
                    "op": "d",
                    "source": {
                        "table": "products"
                    }
                }
                """;

        dataProcessor.process(cdcEvent);

        verify(mockEsWriter, never()).delete(any(), any());
    }

    @Test
    void testProcessEmptyMessage() {
        dataProcessor.process("");

        verify(mockEsWriter, never()).upsert(any(), any(), any());
        verify(mockEsWriter, never()).delete(any(), any());
    }

    @Test
    void testProcessNullMessage() {
        dataProcessor.process(null);

        verify(mockEsWriter, never()).upsert(any(), any(), any());
        verify(mockEsWriter, never()).delete(any(), any());
    }

    @Test
    void testExtractIdWithStringId() {
        String cdcEvent = """
                {
                    "op": "c",
                    "source": {
                        "table": "products"
                    },
                    "after": {
                        "id": "string-id-123",
                        "title": "Product"
                    }
                }
                """;

        dataProcessor.process(cdcEvent);

        ArgumentCaptor<String> idCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockEsWriter).upsert(anyString(), idCaptor.capture(), anyString());
        assertEquals("string-id-123", idCaptor.getValue());
    }

    @Test
    void testExtractIdWithNumericId() {
        String cdcEvent = """
                {
                    "op": "c",
                    "source": {
                        "table": "products"
                    },
                    "after": {
                        "id": 999,
                        "title": "Product"
                    }
                }
                """;

        dataProcessor.process(cdcEvent);

        ArgumentCaptor<String> idCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockEsWriter).upsert(anyString(), idCaptor.capture(), anyString());
        assertEquals("999", idCaptor.getValue());
    }

    @Test
    void testExtractIdWithPrimaryKeyField() {
        String cdcEvent = """
                {
                    "op": "c",
                    "source": {
                        "table": "products"
                    },
                    "after": {
                        "pk": "primary-key-123",
                        "title": "Product"
                    }
                }
                """;

        dataProcessor.process(cdcEvent);

        ArgumentCaptor<String> idCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockEsWriter).upsert(anyString(), idCaptor.capture(), anyString());
        assertEquals("primary-key-123", idCaptor.getValue());
    }

    @Test
    void testExtractIdWithIDField() {
        String cdcEvent = """
                {
                    "op": "c",
                    "source": {
                        "table": "products"
                    },
                    "after": {
                        "_id": "underscore-id-123",
                        "title": "Product"
                    }
                }
                """;

        dataProcessor.process(cdcEvent);

        ArgumentCaptor<String> idCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockEsWriter).upsert(anyString(), idCaptor.capture(), anyString());
        assertEquals("underscore-id-123", idCaptor.getValue());
    }

    @Test
    void testVectorizationTaskEnqueuedForTitleAndDescription() {
        String cdcEvent = """
                {
                    "op": "c",
                    "source": {
                        "table": "products"
                    },
                    "after": {
                        "id": "123",
                        "title": "Test Product",
                        "description": "A great product"
                    }
                }
                """;

        when(mockVectorizationQueue.enqueue(any())).thenReturn(true);

        dataProcessor.process(cdcEvent);

        // Verify vectorization task was enqueued (matches title + description heuristic)
        ArgumentCaptor<VectorizationTask> taskCaptor = ArgumentCaptor.forClass(VectorizationTask.class);
        verify(mockVectorizationQueue, times(1)).enqueue(taskCaptor.capture());

        VectorizationTask enqueuedTask = taskCaptor.getValue();
        assertEquals("products", enqueuedTask.getIndexName());
        assertEquals("123", enqueuedTask.getDocumentId());
        assertEquals("combined_vector", enqueuedTask.getTargetField());
        assertEquals(List.of("title", "description"), enqueuedTask.getSourceFields());
    }

    @Test
    void testVectorizationTaskEnqueuedForContent() {
        String cdcEvent = """
                {
                    "op": "c",
                    "source": {
                        "table": "articles"
                    },
                    "after": {
                        "id": "456",
                        "content": "This is article content"
                    }
                }
                """;

        when(mockVectorizationQueue.enqueue(any())).thenReturn(true);

        dataProcessor.process(cdcEvent);

        ArgumentCaptor<VectorizationTask> taskCaptor = ArgumentCaptor.forClass(VectorizationTask.class);
        verify(mockVectorizationQueue, times(1)).enqueue(taskCaptor.capture());

        VectorizationTask enqueuedTask = taskCaptor.getValue();
        assertEquals("articles", enqueuedTask.getIndexName());
        assertEquals("456", enqueuedTask.getDocumentId());
        assertEquals("content_vector", enqueuedTask.getTargetField());
        assertEquals(List.of("content"), enqueuedTask.getSourceFields());
    }

    @Test
    void testVectorizationDisabled() {
        ReflectionTestUtils.setField(dataProcessor, "vectorizationEnabled", false);

        String cdcEvent = """
                {
                    "op": "c",
                    "source": {
                        "table": "products"
                    },
                    "after": {
                        "id": "123",
                        "title": "Test Product",
                        "description": "A great product"
                    }
                }
                """;

        dataProcessor.process(cdcEvent);

        // Verify ES upsert happened
        verify(mockEsWriter).upsert(any(), any(), any());

        // But vectorization task was NOT enqueued
        verify(mockVectorizationQueue, never()).enqueue(any());
    }

    @Test
    void testVectorizationQueueNull() {
        ReflectionTestUtils.setField(dataProcessor, "vectorizationQueue", null);

        String cdcEvent = """
                {
                    "op": "c",
                    "source": {
                        "table": "products"
                    },
                    "after": {
                        "id": "123",
                        "title": "Test",
                        "description": "Test desc"
                    }
                }
                """;

        // Should not throw exception
        assertDoesNotThrow(() -> dataProcessor.process(cdcEvent));

        // ES upsert should still happen
        verify(mockEsWriter).upsert(any(), any(), any());
    }

    @Test
    void testProcessUnknownOperation() {
        String cdcEvent = """
                {
                    "op": "x",
                    "source": {
                        "table": "products"
                    },
                    "after": {
                        "id": "123"
                    }
                }
                """;

        dataProcessor.process(cdcEvent);

        // Unknown operation should be ignored
        verify(mockEsWriter, never()).upsert(any(), any(), any());
        verify(mockEsWriter, never()).delete(any(), any());
    }

    @Test
    void testProcessInvalidJson() {
        String invalidJson = "{ invalid json }";

        // Should not throw exception, just log error
        assertDoesNotThrow(() -> dataProcessor.process(invalidJson));

        verify(mockEsWriter, never()).upsert(any(), any(), any());
    }
}
