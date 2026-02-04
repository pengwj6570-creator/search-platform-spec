package com.search.vector.service;

import java.util.List;

/**
 * Service for generating text embeddings
 *
 * Implementations can use different models like BGE, GTE, OpenAI, etc.
 */
public interface EmbeddingService {

    /**
     * Generate embedding for a single text
     *
     * @param text the input text
     * @return embedding vector
     */
    float[] embed(String text);

    /**
     * Generate embeddings for multiple texts in batch
     *
     * @param texts list of input texts
     * @return list of embedding vectors
     */
    List<float[]> embedBatch(List<String> texts);

    /**
     * Get the dimension of the embedding vectors
     *
     * @return vector dimension
     */
    int getDimension();

    /**
     * Get the model name/identifier
     *
     * @return model name
     */
    String getModelName();

    /**
     * Check if the service is ready to generate embeddings
     *
     * @return true if ready
     */
    boolean isReady();
}
