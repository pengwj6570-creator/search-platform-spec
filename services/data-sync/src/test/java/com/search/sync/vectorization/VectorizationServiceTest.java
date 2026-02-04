package com.search.sync.vectorization;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for VectorizationService
 */
class VectorizationServiceTest {

    @Mock
    private RestTemplate mockRestTemplate;

    private VectorizationService vectorizationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        vectorizationService = new VectorizationService();
        // Inject the mocked RestTemplate using reflection
        ReflectionTestUtils.setField(vectorizationService, "restTemplate", mockRestTemplate);
        ReflectionTestUtils.setField(vectorizationService, "vectorServiceUrl", "http://localhost:8083");
        ReflectionTestUtils.setField(vectorizationService, "openSearchUrl", "http://localhost:9200");
    }

    @AfterEach
    void tearDown() {
        Mockito.framework().clearInlineMocks();
    }

    private VectorizationTask createTestTask() {
        Map<String, Object> data = new HashMap<>();
        data.put("title", "Test Product");
        data.put("description", "A great product");

        return new VectorizationTask(
                "test_index",
                "doc123",
                List.of("title", "description"),
                "combined_vector",
                data
        );
    }

    @Test
    void testProcessTaskSuccess() {
        // Mock vector service response
        Map<String, Object> vectorResponse = new HashMap<>();
        vectorResponse.put("vector", List.of(0.1f, 0.2f, 0.3f, 0.4f, 0.5f));
        when(mockRestTemplate.postForObject(anyString(), any(), eq(Map.class)))
                .thenReturn(vectorResponse);

        // Mock OpenSearch update response
        when(mockRestTemplate.postForObject(anyString(), anyString(), eq(String.class)))
                .thenReturn("{\"result\": \"updated\"}");

        VectorizationTask task = createTestTask();
        boolean result = vectorizationService.processTask(task);

        assertTrue(result);
        verify(mockRestTemplate, times(1)).postForObject(
                eq("http://localhost:8083/api/v1/embed"),
                any(),
                eq(Map.class)
        );
        verify(mockRestTemplate, times(1)).postForObject(
                contains("test_index"),
                anyString(),
                eq(String.class)
        );
    }

    @Test
    void testProcessTaskWithEmptyCombinedText() {
        Map<String, Object> data = new HashMap<>();
        data.put("title", "");
        data.put("description", "");

        VectorizationTask task = new VectorizationTask(
                "test_index",
                "doc123",
                List.of("title", "description"),
                "combined_vector",
                data
        );

        boolean result = vectorizationService.processTask(task);

        // Should return true (not an error, just nothing to do)
        assertTrue(result);
        // Should NOT call the vector service
        verify(mockRestTemplate, never()).postForObject(
                eq("http://localhost:8083/api/v1/embed"),
                any(),
                any()
        );
    }

    @Test
    void testProcessTaskVectorServiceFailure() {
        when(mockRestTemplate.postForObject(anyString(), any(), eq(Map.class)))
                .thenThrow(new RestClientException("Connection refused"));

        VectorizationTask task = createTestTask();
        boolean result = vectorizationService.processTask(task);

        assertFalse(result);
    }

    @Test
    void testProcessTaskWithNullVectorResponse() {
        when(mockRestTemplate.postForObject(anyString(), any(), eq(Map.class)))
                .thenReturn(null);

        VectorizationTask task = createTestTask();
        boolean result = vectorizationService.processTask(task);

        assertFalse(result);
    }

    @Test
    void testProcessTaskWithEmptyVectorResponse() {
        Map<String, Object> vectorResponse = new HashMap<>();
        // Missing "vector" key
        when(mockRestTemplate.postForObject(anyString(), any(), eq(Map.class)))
                .thenReturn(vectorResponse);

        VectorizationTask task = createTestTask();
        boolean result = vectorizationService.processTask(task);

        assertFalse(result);
    }

    @Test
    void testProcessTaskOpenSearchFailure() {
        // Mock successful vector response
        Map<String, Object> vectorResponse = new HashMap<>();
        vectorResponse.put("vector", List.of(0.1f, 0.2f, 0.3f));

        when(mockRestTemplate.postForObject(anyString(), any(), eq(Map.class))
                .thenReturn(vectorResponse);

        // Mock OpenSearch failure
        when(mockRestTemplate.postForObject(anyString(), anyString(), eq(String.class)))
                .thenThrow(new RestClientException("OpenSearch unavailable"));

        VectorizationTask task = createTestTask();
        boolean result = vectorizationService.processTask(task);

        assertFalse(result);
    }

    @Test
    void testIsVectorServiceAvailableSuccess() {
        Map<String, Object> healthResponse = new HashMap<>();
        healthResponse.put("status", "UP");

        when(mockRestTemplate.getForObject(anyString(), eq(Map.class)))
                .thenReturn(healthResponse);

        boolean available = vectorizationService.isVectorServiceAvailable();

        assertTrue(available);
        verify(mockRestTemplate, times(1)).getForObject(
                eq("http://localhost:8083/actuator/health"),
                eq(Map.class)
        );
    }

    @Test
    void testIsVectorServiceAvailableDown() {
        Map<String, Object> healthResponse = new HashMap<>();
        healthResponse.put("status", "DOWN");

        when(mockRestTemplate.getForObject(anyString(), eq(Map.class)))
                .thenReturn(healthResponse);

        boolean available = vectorizationService.isVectorServiceAvailable();

        assertFalse(available);
    }

    @Test
    void testIsVectorServiceAvailableException() {
        when(mockRestTemplate.getForObject(anyString(), eq(Map.class)))
                .thenThrow(new RestClientException("Service unavailable"));

        boolean available = vectorizationService.isVectorServiceAvailable();

        assertFalse(available);
    }

    @Test
    void testIsVectorServiceAvailableNullResponse() {
        when(mockRestTemplate.getForObject(anyString(), eq(Map.class)))
                .thenReturn(null);

        boolean available = vectorizationService.isVectorServiceAvailable();

        assertFalse(available);
    }

    @Test
    void testProcessTaskWithSingleField() {
        Map<String, Object> vectorResponse = new HashMap<>();
        vectorResponse.put("vector", List.of(0.1f, 0.2f, 0.3f));

        when(mockRestTemplate.postForObject(anyString(), any(), eq(Map.class)))
                .thenReturn(vectorResponse);
        when(mockRestTemplate.postForObject(anyString(), anyString(), eq(String.class)))
                .thenReturn("{\"result\": \"updated\"}");

        Map<String, Object> data = new HashMap<>();
        data.put("title", "Single Field Product");

        VectorizationTask task = new VectorizationTask(
                "test_index",
                "doc123",
                List.of("title"),
                "title_vector",
                data
        );

        boolean result = vectorizationService.processTask(task);

        assertTrue(result);
    }

    @Test
    void testProcessTaskWithNumericFieldValue() {
        Map<String, Object> vectorResponse = new HashMap<>();
        vectorResponse.put("vector", List.of(0.1f, 0.2f));

        when(mockRestTemplate.postForObject(anyString(), any(), eq(Map.class)))
                .thenReturn(vectorResponse);
        when(mockRestTemplate.postForObject(anyString(), anyString(), eq(String.class)))
                .thenReturn("{\"result\": \"updated\"}");

        Map<String, Object> data = new HashMap<>();
        data.put("price", 99.99);

        VectorizationTask task = new VectorizationTask(
                "test_index",
                "doc123",
                List.of("price"),
                "price_vector",
                data
        );

        boolean result = vectorizationService.processTask(task);

        assertTrue(result);
    }

    @Test
    void testUpdateDocumentVectorUsesCorrectUrl() {
        Map<String, Object> vectorResponse = new HashMap<>();
        vectorResponse.put("vector", List.of(0.1f));

        when(mockRestTemplate.postForObject(anyString(), any(), eq(Map.class)))
                .thenReturn(vectorResponse);
        when(mockRestTemplate.postForObject(anyString(), anyString(), eq(String.class)))
                .thenReturn("{\"result\": \"updated\"}");

        VectorizationTask task = new VectorizationTask(
                "my_index",
                "doc456",
                List.of("title"),
                "my_vector",
                new HashMap<>()
        );

        vectorizationService.processTask(task);

        // Verify the OpenSearch URL construction
        verify(mockRestTemplate).postForObject(
                eq("http://localhost:9200/my_index/_update/doc456"),
                anyString(),
                eq(String.class)
        );
    }

    @Test
    void testVectorServiceUrlConfiguration() {
        ReflectionTestUtils.setField(vectorizationService, "vectorServiceUrl", "http://custom-vector:8080");

        Map<String, Object> healthResponse = new HashMap<>();
        healthResponse.put("status", "UP");

        when(mockRestTemplate.getForObject(anyString(), eq(Map.class)))
                .thenReturn(healthResponse);

        vectorizationService.isVectorServiceAvailable();

        verify(mockRestTemplate).getForObject(
                eq("http://custom-vector:8080/actuator/health"),
                eq(Map.class)
        );
    }

    @Test
    void testOpenSearchUrlConfiguration() {
        ReflectionTestUtils.setField(vectorizationService, "openSearchUrl", "http://custom-opensearch:9200");

        Map<String, Object> vectorResponse = new HashMap<>();
        vectorResponse.put("vector", List.of(0.1f));

        when(mockRestTemplate.postForObject(anyString(), any(), eq(Map.class)))
                .thenReturn(vectorResponse);
        when(mockRestTemplate.postForObject(anyString(), anyString(), eq(String.class)))
                .thenReturn("{\"result\": \"updated\"}");

        VectorizationTask task = new VectorizationTask(
                "my_index",
                "doc123",
                List.of("title"),
                "vector",
                new HashMap<>()
        );

        vectorizationService.processTask(task);

        verify(mockRestTemplate).postForObject(
                eq("http://custom-opensearch:9200/my_index/_update/doc123"),
                anyString(),
                eq(String.class)
        );
    }
}
