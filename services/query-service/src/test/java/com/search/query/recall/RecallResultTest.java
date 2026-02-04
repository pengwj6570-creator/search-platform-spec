package com.search.query.recall;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for RecallResult
 */
class RecallResultTest {

    @Test
    void testRecallResultCreation() {
        RecallResult result = new RecallResult("doc123", 0.95f, "keyword");

        assertEquals("doc123", result.getId());
        assertEquals(0.95f, result.getScore(), 0.001);
        assertEquals("keyword", result.getSource());
    }

    @Test
    void testRecallResultWithZeroScore() {
        RecallResult result = new RecallResult("doc456", 0.0f, "hot");

        assertEquals("doc456", result.getId());
        assertEquals(0.0f, result.getScore(), 0.001);
        assertEquals("hot", result.getSource());
    }

    @Test
    void testRecallResultWithNegativeScore() {
        RecallResult result = new RecallResult("doc789", -0.5f, "vector");

        assertEquals("doc789", result.getId());
        assertEquals(-0.5f, result.getScore(), 0.001);
        assertEquals("vector", result.getSource());
    }

    @Test
    void testEqualsSameId() {
        RecallResult result1 = new RecallResult("doc1", 0.8f, "keyword");
        RecallResult result2 = new RecallResult("doc1", 0.5f, "vector");

        assertEquals(result1, result2);
        assertEquals(result1.hashCode(), result2.hashCode());
    }

    @Test
    void testEqualsDifferentId() {
        RecallResult result1 = new RecallResult("doc1", 0.8f, "keyword");
        RecallResult result2 = new RecallResult("doc2", 0.8f, "keyword");

        assertNotEquals(result1, result2);
    }

    @Test
    void testEqualsWithNull() {
        RecallResult result = new RecallResult("doc1", 0.8f, "keyword");

        assertNotEquals(result, null);
    }

    @Test
    void testEqualsWithDifferentClass() {
        RecallResult result = new RecallResult("doc1", 0.8f, "keyword");

        assertNotEquals(result, "doc1");
    }

    @Test
    void testEqualsWithItself() {
        RecallResult result = new RecallResult("doc1", 0.8f, "keyword");

        assertEquals(result, result);
    }

    @Test
    void testHashCodeConsistency() {
        RecallResult result = new RecallResult("doc1", 0.8f, "keyword");

        int hashCode1 = result.hashCode();
        int hashCode2 = result.hashCode();

        assertEquals(hashCode1, hashCode2);
    }

    @Test
    void testToString() {
        RecallResult result = new RecallResult("doc123", 0.95f, "keyword");

        String str = result.toString();

        assertTrue(str.contains("doc123"));
        assertTrue(str.contains("0.95"));
        assertTrue(str.contains("keyword"));
    }

    @Test
    void testRecallResultWithVectorSource() {
        RecallResult result = new RecallResult("vec_123", 0.87f, "vector");

        assertEquals("vec_123", result.getId());
        assertEquals(0.87f, result.getScore(), 0.001);
        assertEquals("vector", result.getSource());
    }

    @Test
    void testRecallResultWithHotSource() {
        RecallResult result = new RecallResult("hot_456", 1.0f, "hot");

        assertEquals("hot_456", result.getId());
        assertEquals(1.0f, result.getScore(), 0.001);
        assertEquals("hot", result.getSource());
    }
}
