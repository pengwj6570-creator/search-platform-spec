package com.search.query.recall;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

/**
 * Simple embedding service for development/testing
 *
 * This is a placeholder implementation that generates simple hash-based
 * embeddings. In production, this should be replaced with actual embedding
 * models (BGE, GTE, OpenAI embeddings, etc.)
 */
@Service
public class SimpleEmbeddingService implements VectorEmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(SimpleEmbeddingService.class);

    private static final int DEFAULT_DIMENSION = 768;

    @Override
    public float[] embed(String text) {
        if (text == null || text.isEmpty()) {
            return new float[DEFAULT_DIMENSION];
        }

        // Simple hash-based embedding for development
        // In production, use actual embedding models
        float[] embedding = new float[DEFAULT_DIMENSION];
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);

        for (int i = 0; i < DEFAULT_DIMENSION; i++) {
            int hash = (bytes[i % bytes.length] * 31 + i) % 100;
            embedding[i] = hash / 100.0f;
        }

        // Normalize
        float norm = 0;
        for (float v : embedding) {
            norm += v * v;
        }
        norm = (float) Math.sqrt(norm);

        if (norm > 0) {
            for (int i = 0; i < DEFAULT_DIMENSION; i++) {
                embedding[i] /= norm;
            }
        }

        log.debug("Generated embedding for text: {} (dimension={})", text.substring(0, Math.min(20, text.length())), DEFAULT_DIMENSION);

        return embedding;
    }

    @Override
    public int getDimension() {
        return DEFAULT_DIMENSION;
    }
}
