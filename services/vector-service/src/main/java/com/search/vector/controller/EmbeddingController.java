package com.search.vector.controller;

import com.search.vector.service.EmbeddingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for text embedding operations
 */
@RestController
@RequestMapping("/api/v1/embedding")
public class EmbeddingController {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingController.class);

    private final EmbeddingService embeddingService;

    public EmbeddingController(EmbeddingService embeddingService) {
        this.embeddingService = embeddingService;
    }

    /**
     * Generate embedding for a single text
     *
     * POST /api/v1/embedding
     *
     * @param request embedding request
     * @return embedding response with vector
     */
    @PostMapping
    public ResponseEntity<EmbeddingResponse> embed(@RequestBody EmbeddingRequest request) {
        try {
            if (!embeddingService.isReady()) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
            }

            long startTime = System.currentTimeMillis();
            float[] vector = embeddingService.embed(request.getText());
            long took = System.currentTimeMillis() - startTime;

            EmbeddingResponse response = new EmbeddingResponse(
                    vector,
                    embeddingService.getDimension(),
                    embeddingService.getModelName(),
                    took
            );

            log.info("Generated embedding: text_length={}, dimension={}, took={}ms",
                    request.getText() != null ? request.getText().length() : 0,
                    vector.length, took);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to generate embedding", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Generate embeddings for multiple texts in batch
     *
     * POST /api/v1/embedding/batch
     *
     * @param request batch embedding request
     * @return batch embedding response
     */
    @PostMapping("/batch")
    public ResponseEntity<BatchEmbeddingResponse> embedBatch(@RequestBody BatchEmbeddingRequest request) {
        try {
            if (!embeddingService.isReady()) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
            }

            long startTime = System.currentTimeMillis();
            List<float[]> vectors = embeddingService.embedBatch(request.getTexts());
            long took = System.currentTimeMillis() - startTime;

            BatchEmbeddingResponse response = new BatchEmbeddingResponse(
                    vectors,
                    embeddingService.getDimension(),
                    embeddingService.getModelName(),
                    took
            );

            log.info("Generated batch embeddings: count={}, dimension={}, took={}ms",
                    vectors.size(), embeddingService.getDimension(), took);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to generate batch embeddings", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get model information
     *
     * GET /api/v1/embedding/info
     *
     * @return model information
     */
    @GetMapping("/info")
    public ResponseEntity<ModelInfo> getModelInfo() {
        ModelInfo info = new ModelInfo(
                embeddingService.getModelName(),
                embeddingService.getDimension(),
                embeddingService.isReady()
        );
        return ResponseEntity.ok(info);
    }

    /**
     * Health check
     *
     * GET /api/v1/embedding/health
     *
     * @return health status
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", embeddingService.isReady() ? "UP" : "DOWN");
        health.put("model", embeddingService.getModelName());
        health.put("dimension", embeddingService.getDimension());
        return ResponseEntity.ok(health);
    }

    // Request/Response models

    public static class EmbeddingRequest {
        private String text;

        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
    }

    public static class BatchEmbeddingRequest {
        private List<String> texts;

        public List<String> getTexts() { return texts; }
        public void setTexts(List<String> texts) { this.texts = texts; }
    }

    public static class EmbeddingResponse {
        private float[] vector;
        private int dimension;
        private String model;
        private long took;

        public EmbeddingResponse(float[] vector, int dimension, String model, long took) {
            this.vector = vector;
            this.dimension = dimension;
            this.model = model;
            this.took = took;
        }

        public float[] getVector() { return vector; }
        public int getDimension() { return dimension; }
        public String getModel() { return model; }
        public long getTook() { return took; }
    }

    public static class BatchEmbeddingResponse {
        private List<float[]> vectors;
        private int dimension;
        private String model;
        private long took;

        public BatchEmbeddingResponse(List<float[]> vectors, int dimension, String model, long took) {
            this.vectors = vectors;
            this.dimension = dimension;
            this.model = model;
            this.took = took;
        }

        public List<float[]> getVectors() { return vectors; }
        public int getDimension() { return dimension; }
        public String getModel() { return model; }
        public long getTook() { return took; }
    }

    public static class ModelInfo {
        private String model;
        private int dimension;
        private boolean ready;

        public ModelInfo(String model, int dimension, boolean ready) {
            this.model = model;
            this.dimension = dimension;
            this.ready = ready;
        }

        public String getModel() { return model; }
        public int getDimension() { return dimension; }
        public boolean isReady() { return ready; }
    }
}
