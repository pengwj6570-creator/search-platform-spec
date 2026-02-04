package com.search.vector.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Service for generating image embeddings
 *
 * Supports CLIP and other vision-language models for:
 * - Image-to-image search (similar images)
 * - Text-to-image search (image retrieval by text)
 */
@Service
public class ImageEmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(ImageEmbeddingService.class);

    @Value("${embedding.image.model:clip-vit-base-patch32}")
    private String modelName;

    @Value("${embedding.image.dimension:512}")
    private int dimension;

    @Value("${embedding.use-model:false}")
    private boolean useModel;

    /**
     * Generate embedding from uploaded image file
     *
     * @param file the image file
     * @return embedding vector
     */
    public float[] embed(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            log.warn("Empty file provided for image embedding");
            return new float[dimension];
        }

        try {
            byte[] imageData = file.getBytes();
            return embed(imageData);

        } catch (IOException e) {
            log.error("Failed to read image file", e);
            return new float[dimension];
        }
    }

    /**
     * Generate embedding from image bytes
     *
     * @param imageData the image data
     * @return embedding vector
     */
    public float[] embed(byte[] imageData) {
        if (imageData == null || imageData.length == 0) {
            return new float[dimension];
        }

        if (useModel) {
            return embedWithModel(imageData);
        }

        // Simple hash-based embedding for development
        return embedWithHash(imageData);
    }

    /**
     * Generate embedding from image URL
     *
     * @param imageUrl the image URL
     * @return embedding vector
     */
    public float[] embedFromUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return new float[dimension];
        }

        // For URL-based embedding, would need to download the image first
        // For now, use hash of URL as embedding
        return embedWithHash(imageUrl.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Get the embedding dimension
     *
     * @return vector dimension
     */
    public int getDimension() {
        return dimension;
    }

    /**
     * Get the model name
     *
     * @return model name
     */
    public String getModelName() {
        return modelName;
    }

    /**
     * Check if service is ready
     *
     * @return true if ready
     */
    public boolean isReady() {
        return true;
    }

    /**
     * Generate hash-based embedding for development
     *
     * Note: This is NOT a real image embedding.
     * For production, use CLIP or other vision models.
     */
    private float[] embedWithHash(byte[] data) {
        float[] embedding = new float[dimension];

        // Generate pseudo-random but deterministic values from image data
        int sampleSize = Math.min(data.length, 1000);
        int step = Math.max(1, data.length / dimension);

        for (int i = 0; i < dimension; i++) {
            int idx = (i * step) % data.length;
            int hash = data[idx] & 0xFF; // unsigned byte
            // Normalize to [-1, 1]
            embedding[i] = ((hash / 255.0f) * 2) - 1;
        }

        // L2 normalize
        float norm = 0;
        for (float v : embedding) {
            norm += v * v;
        }
        norm = (float) Math.sqrt(norm);

        if (norm > 0) {
            for (int i = 0; i < dimension; i++) {
                embedding[i] /= norm;
            }
        }

        return embedding;
    }

    /**
     * Generate embedding using actual model
     *
     * This would use CLIP or other vision models via DJL.
     * For now, it falls back to hash-based embedding.
     */
    private float[] embedWithModel(byte[] imageData) {
        // TODO: Implement actual CLIP model loading and inference
        // This would use DJL to load CLIP models from HuggingFace
        log.warn("Image model inference not yet implemented, using hash-based embedding");
        return embedWithHash(imageData);
    }
}
