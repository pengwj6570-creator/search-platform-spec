package com.search.query.recall;

/**
 * Service for generating text embeddings
 *
 * This interface can be implemented to call external embedding services
 * or load models locally (e.g., BGE, GTE, CLIP).
 */
public interface VectorEmbeddingService {

    /**
     * Generate embedding for a text query
     *
     * @param text the input text
     * @return embedding vector
     */
    float[] embed(String text);

    /**
     * Get the dimension of the embedding vectors
     *
     * @return vector dimension
     */
    int getDimension();
}
