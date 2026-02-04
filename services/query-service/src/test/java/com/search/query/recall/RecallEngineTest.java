package com.search.query.recall;

import com.search.query.model.SearchRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RecallEngine
 */
class RecallEngineTest {

    @Mock
    private KeywordRecall mockKeywordRecall;

    @Mock
    private VectorRecall mockVectorRecall;

    @Mock
    private HotRecall mockHotRecall;

    private RecallEngine recallEngine;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        recallEngine = new RecallEngine(mockKeywordRecall, mockVectorRecall, mockHotRecall);
    }

    @AfterEach
    void tearDown() {
        if (recallEngine != null) {
            recallEngine.shutdown();
        }
    }

    private List<RecallResult> createMockResults(String source, int count) {
        List<RecallResult> results = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            results.add(new RecallResult(source + "_id_" + i, 1.0f - (i * 0.1f), source));
        }
        return results;
    }

    private SearchRequest createSearchRequest(String query, boolean keyword, boolean vector, boolean hot) {
        SearchRequest request = new SearchRequest();
        request.setQuery(query);

        SearchRequest.RecallStrategy strategy = new SearchRequest.RecallStrategy();
        strategy.setKeyword(keyword);
        strategy.setHot(hot);

        if (vector) {
            SearchRequest.VectorConfig vectorConfig = new SearchRequest.VectorConfig();
            vectorConfig.setEnabled(true);
            vectorConfig.setK(50);
            strategy.setVector(vectorConfig);
        }

        request.setRecallStrategy(strategy);
        return request;
    }

    @Test
    void testRecallWithOnlyKeyword() {
        String index = "test_index";
        String query = "test query";
        SearchRequest request = createSearchRequest(query, true, false, false);

        List<RecallResult> keywordResults = createMockResults("keyword", 5);
        when(mockKeywordRecall.recall(eq(index), eq(query), eq(100)))
                .thenReturn(keywordResults);

        List<RecallResult> results = recallEngine.recall(index, request);

        assertNotNull(results);
        assertEquals(5, results.size());

        verify(mockKeywordRecall).recall(eq(index), eq(query), eq(100));
        verify(mockVectorRecall, never()).recall(any(), any(), any(), anyInt());
        verify(mockHotRecall, never()).recall(any(), any(), anyInt());
    }

    @Test
    void testRecallWithOnlyVector() {
        String index = "test_index";
        String query = "semantic search";
        SearchRequest request = createSearchRequest(query, false, true, false);

        List<RecallResult> vectorResults = createMockResults("vector", 10);
        when(mockVectorRecall.recall(eq(index), eq(query), anyString(), eq(50)))
                .thenReturn(vectorResults);

        List<RecallResult> results = recallEngine.recall(index, request);

        assertNotNull(results);
        assertEquals(10, results.size());

        verify(mockKeywordRecall, never()).recall(any(), any(), anyInt());
        verify(mockVectorRecall).recall(eq(index), eq(query), anyString(), eq(50));
        verify(mockHotRecall, never()).recall(any(), any(), anyInt());
    }

    @Test
    void testRecallWithOnlyHot() {
        String index = "test_index";
        SearchRequest request = createSearchRequest(null, false, false, true);

        List<RecallResult> hotResults = createMockResults("hot", 3);
        when(mockHotRecall.recall(eq(index), eq("sales"), eq(50)))
                .thenReturn(hotResults);

        List<RecallResult> results = recallEngine.recall(index, request);

        assertNotNull(results);
        assertEquals(3, results.size());

        verify(mockKeywordRecall, never()).recall(any(), any(), anyInt());
        verify(mockVectorRecall, never()).recall(any(), any(), any(), anyInt());
        verify(mockHotRecall).recall(eq(index), eq("sales"), eq(50));
    }

    @Test
    void testRecallWithAllStrategies() {
        String index = "test_index";
        String query = "laptop";
        SearchRequest request = createSearchRequest(query, true, true, true);

        List<RecallResult> keywordResults = createMockResults("keyword", 5);
        List<RecallResult> vectorResults = createMockResults("vector", 8);
        List<RecallResult> hotResults = createMockResults("hot", 3);

        when(mockKeywordRecall.recall(eq(index), eq(query), eq(100)))
                .thenReturn(keywordResults);
        when(mockVectorRecall.recall(eq(index), eq(query), anyString(), eq(50)))
                .thenReturn(vectorResults);
        when(mockHotRecall.recall(eq(index), eq("sales"), eq(50)))
                .thenReturn(hotResults);

        List<RecallResult> results = recallEngine.recall(index, request);

        assertNotNull(results);
        assertEquals(16, results.size()); // 5 + 8 + 3

        verify(mockKeywordRecall).recall(eq(index), eq(query), eq(100));
        verify(mockVectorRecall).recall(eq(index), eq(query), anyString(), eq(50));
        verify(mockHotRecall).recall(eq(index), eq("sales"), eq(50));
    }

    @Test
    void testRecallWithNullStrategy() {
        String index = "test_index";
        SearchRequest request = new SearchRequest();
        request.setQuery("test");
        request.setRecallStrategy(null);

        List<RecallResult> keywordResults = createMockResults("keyword", 2);
        when(mockKeywordRecall.recall(eq(index), eq("test"), eq(100)))
                .thenReturn(keywordResults);

        List<RecallResult> results = recallEngine.recall(index, request);

        assertNotNull(results);
        assertEquals(2, results.size());
    }

    @Test
    void testRecallWithFilters() {
        String index = "test_index";
        String query = "phone";
        SearchRequest request = createSearchRequest(query, true, false, false);

        Map<String, Object> filters = new HashMap<>();
        filters.put("category", "electronics");
        request.setFilters(filters);

        List<RecallResult> keywordResults = createMockResults("keyword", 4);
        when(mockKeywordRecall.recallWithFilters(eq(index), eq(query), eq(filters), eq(100)))
                .thenReturn(keywordResults);

        List<RecallResult> results = recallEngine.recall(index, request);

        assertNotNull(results);
        assertEquals(4, results.size());

        verify(mockKeywordRecall).recallWithFilters(eq(index), eq(query), eq(filters), eq(100));
        verify(mockKeywordRecall, never()).recall(any(), any(), anyInt());
    }

    @Test
    void testRecallHandlesExceptionInKeyword() {
        String index = "test_index";
        SearchRequest request = createSearchRequest("test", true, false, false);

        when(mockKeywordRecall.recall(any(), any(), anyInt()))
                .thenThrow(new RuntimeException("OpenSearch error"));

        List<RecallResult> results = recallEngine.recall(index, request);

        // Should return empty list on error
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void testRecallHandlesExceptionInVector() {
        String index = "test_index";
        SearchRequest request = createSearchRequest("test", true, true, false);

        when(mockKeywordRecall.recall(any(), any(), anyInt()))
                .thenReturn(createMockResults("keyword", 2));
        when(mockVectorRecall.recall(any(), any(), any(), anyInt()))
                .thenThrow(new RuntimeException("Vector service error"));

        List<RecallResult> results = recallEngine.recall(index, request);

        // Should return only keyword results
        assertNotNull(results);
        assertEquals(2, results.size());
    }

    @Test
    void testRecallWithEmptyResults() {
        String index = "test_index";
        SearchRequest request = createSearchRequest("test", true, true, true);

        when(mockKeywordRecall.recall(any(), any(), anyInt()))
                .thenReturn(List.of());
        when(mockVectorRecall.recall(any(), any(), any(), anyInt()))
                .thenReturn(List.of());
        when(mockHotRecall.recall(any(), any(), anyInt()))
                .thenReturn(List.of());

        List<RecallResult> results = recallEngine.recall(index, request);

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void testKeywordRecallSinglePath() {
        String index = "products";
        String query = "shoes";

        List<RecallResult> expectedResults = createMockResults("keyword", 10);
        when(mockKeywordRecall.recall(eq(index), eq(query), eq(20)))
                .thenReturn(expectedResults);

        List<RecallResult> results = recallEngine.keywordRecall(index, query, null, 20);

        assertEquals(10, results.size());
        verify(mockKeywordRecall).recall(eq(index), eq(query), eq(20));
    }

    @Test
    void testKeywordRecallWithFiltersSinglePath() {
        String index = "products";
        String query = "shoes";
        Map<String, Object> filters = new HashMap<>();
        filters.put("brand", "Nike");

        List<RecallResult> expectedResults = createMockResults("keyword", 5);
        when(mockKeywordRecall.recallWithFilters(eq(index), eq(query), eq(filters), eq(20)))
                .thenReturn(expectedResults);

        List<RecallResult> results = recallEngine.keywordRecall(index, query, filters, 20);

        assertEquals(5, results.size());
        verify(mockKeywordRecall).recallWithFilters(eq(index), eq(query), eq(filters), eq(20));
    }

    @Test
    void testVectorRecallSinglePath() {
        String index = "products";
        String query = "running shoes";
        String vectorField = "product_vector";

        List<RecallResult> expectedResults = createMockResults("vector", 15);
        when(mockVectorRecall.recall(eq(index), eq(query), eq(vectorField), eq(30)))
                .thenReturn(expectedResults);

        List<RecallResult> results = recallEngine.vectorRecall(index, query, vectorField, 30);

        assertEquals(15, results.size());
        verify(mockVectorRecall).recall(eq(index), eq(query), eq(vectorField), eq(30));
    }

    @Test
    void testHotRecallSinglePath() {
        String index = "products";
        String sortField = "sales";

        List<RecallResult> expectedResults = createMockResults("hot", 7);
        when(mockHotRecall.recall(eq(index), eq(sortField), eq(25)))
                .thenReturn(expectedResults);

        List<RecallResult> results = recallEngine.hotRecall(index, sortField, 25);

        assertEquals(7, results.size());
        verify(mockHotRecall).recall(eq(index), eq(sortField), eq(25));
    }

    @Test
    void testRecallDeduplicatesResults() {
        String index = "test_index";
        SearchRequest request = createSearchRequest("test", true, true, false);

        // Create results with duplicate IDs
        List<RecallResult> keywordResults = List.of(
                new RecallResult("doc1", 1.0f, "keyword"),
                new RecallResult("doc2", 0.9f, "keyword"),
                new RecallResult("doc3", 0.8f, "keyword")
        );

        List<RecallResult> vectorResults = List.of(
                new RecallResult("doc1", 0.95f, "vector"), // Duplicate ID
                new RecallResult("doc4", 0.85f, "vector"),
                new RecallResult("doc5", 0.75f, "vector")
        );

        when(mockKeywordRecall.recall(any(), any(), anyInt()))
                .thenReturn(keywordResults);
        when(mockVectorRecall.recall(any(), any(), any(), anyInt()))
                .thenReturn(vectorResults);

        List<RecallResult> results = recallEngine.recall(index, request);

        // Should return all results (RecallEngine doesn't deduplicate in current implementation)
        assertNotNull(results);
        assertEquals(6, results.size());
    }

    @Test
    void testRecallWithEmptyQuery() {
        String index = "test_index";
        SearchRequest request = createSearchRequest(null, true, false, false);

        // With null query, keyword recall should not be called
        List<RecallResult> results = recallEngine.recall(index, request);

        assertNotNull(results);
        assertTrue(results.isEmpty());
        verify(mockKeywordRecall, never()).recall(any(), any(), anyInt());
    }

    @Test
    void testRecallWithVectorDisabled() {
        String index = "test_index";
        SearchRequest request = createSearchRequest("test", true, false, false);

        List<RecallResult> keywordResults = createMockResults("keyword", 3);
        when(mockKeywordRecall.recall(any(), any(), anyInt()))
                .thenReturn(keywordResults);

        List<RecallResult> results = recallEngine.recall(index, request);

        assertEquals(3, results.size());
        verify(mockVectorRecall, never()).recall(any(), any(), any(), anyInt());
    }

    @Test
    void testShutdown() {
        // Verify shutdown doesn't throw exception
        assertDoesNotThrow(() -> recallEngine.shutdown());
    }

    @Test
    void testRecallWithCustomVectorK() {
        String index = "test_index";
        SearchRequest request = createSearchRequest("test", true, true, false);

        SearchRequest.VectorConfig vectorConfig = new SearchRequest.VectorConfig();
        vectorConfig.setEnabled(true);
        vectorConfig.setK(100); // Custom K value
        request.getRecallStrategy().setVector(vectorConfig);

        when(mockKeywordRecall.recall(any(), any(), anyInt()))
                .thenReturn(createMockResults("keyword", 1));
        when(mockVectorRecall.recall(any(), any(), any(), eq(100)))
                .thenReturn(createMockResults("vector", 1));

        recallEngine.recall(index, request);

        verify(mockVectorRecall).recall(any(), any(), any(), eq(100));
    }
}
