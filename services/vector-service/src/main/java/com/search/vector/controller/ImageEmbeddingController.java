package com.search.vector.controller;

import com.search.vector.service.ImageEmbeddingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for image embedding operations
 */
@RestController
@RequestMapping("/api/v1/embedding/image")
public class ImageEmbeddingController {

    private static final Logger log = LoggerFactory.getLogger(ImageEmbeddingController.class);

    private final ImageEmbeddingService imageEmbeddingService;

    public ImageEmbeddingController(ImageEmbeddingService imageEmbeddingService) {
        this.imageEmbeddingService = imageEmbeddingService;
    }

    /**
     * Generate embedding from uploaded image
     *
     * POST /api/v1/embedding/image
     *
     * @param file the image file
     * @return embedding response
     */
    @PostMapping
    public ResponseEntity<ImageEmbeddingResponse> embedImage(@RequestParam("file") MultipartFile file) {
        try {
            if (!imageEmbeddingService.isReady()) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
            }

            // Validate file type
            String contentType = file.getContentType();
            if (contentType == null || (!contentType.startsWith("image/"))) {
                return ResponseEntity.badRequest().build();
            }

            long startTime = System.currentTimeMillis();
            float[] vector = imageEmbeddingService.embed(file);
            long took = System.currentTimeMillis() - startTime;

            ImageEmbeddingResponse response = new ImageEmbeddingResponse(
                    file.getOriginalFilename(),
                    vector,
                    imageEmbeddingService.getDimension(),
                    imageEmbeddingService.getModelName(),
                    took
            );

            log.info("Generated image embedding: filename={}, size={}, dimension={}, took={}ms",
                    file.getOriginalFilename(), file.getSize(), vector.length, took);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to generate image embedding", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Generate embedding from image URL
     *
     * POST /api/v1/embedding/image/url
     *
     * @param request URL request
     * @return embedding response
     */
    @PostMapping("/url")
    public ResponseEntity<ImageEmbeddingResponse> embedImageUrl(@RequestBody ImageUrlRequest request) {
        try {
            if (!imageEmbeddingService.isReady()) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
            }

            if (request.getUrl() == null || request.getUrl().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            long startTime = System.currentTimeMillis();
            float[] vector = imageEmbeddingService.embedFromUrl(request.getUrl());
            long took = System.currentTimeMillis() - startTime;

            ImageEmbeddingResponse response = new ImageEmbeddingResponse(
                    request.getUrl(),
                    vector,
                    imageEmbeddingService.getDimension(),
                    imageEmbeddingService.getModelName(),
                    took
            );

            log.info("Generated image embedding from URL: url={}, dimension={}, took={}ms",
                    request.getUrl(), vector.length, took);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to generate image embedding from URL", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get image model information
     *
     * GET /api/v1/embedding/image/info
     *
     * @return model information
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getModelInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("model", imageEmbeddingService.getModelName());
        info.put("dimension", imageEmbeddingService.getDimension());
        info.put("ready", imageEmbeddingService.isReady());
        info.put("type", "image");
        return ResponseEntity.ok(info);
    }

    // Request/Response models

    public static class ImageUrlRequest {
        private String url;

        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
    }

    public static class ImageEmbeddingResponse {
        private String source;
        private float[] vector;
        private int dimension;
        private String model;
        private long took;

        public ImageEmbeddingResponse(String source, float[] vector, int dimension, String model, long took) {
            this.source = source;
            this.vector = vector;
            this.dimension = dimension;
            this.model = model;
            this.took = took;
        }

        public String getSource() { return source; }
        public float[] getVector() { return vector; }
        public int getDimension() { return dimension; }
        public String getModel() { return model; }
        public long getTook() { return took; }
    }
}
