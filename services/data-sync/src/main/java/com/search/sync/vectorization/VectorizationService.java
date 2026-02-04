package com.search.sync.vectorization;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.opensearch.client.opensearch.core.UpdateRequest;
import org.opensearch.client.opensearch.core.UpdateResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for asynchronous vectorization of document fields
 *
 * Integrates with the vector-service for embedding generation
 * and updates OpenSearch documents with vector fields.
 */
@Service
public class VectorizationService {

    private static final Logger log = LoggerFactory.getLogger(VectorizationService.class);

    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${vector.service.url:http://localhost:8083}")
    private String vectorServiceUrl;

    @Value("${opensearch.url:http://localhost:9200}")
    private String openSearchUrl;

    private final RestTemplate restTemplate;

    public VectorizationService() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Process a vectorization task
     *
     * @param task the vectorization task
     * @return true if successful
     */
    public boolean processTask(VectorizationTask task) {
        try {
            // 1. Combine text from source fields
            String combinedText = task.combineText();
            if (combinedText.isEmpty()) {
                log.warn("No text to vectorize for task: {}", task);
                return true; // Not an error, just nothing to do
            }

            // 2. Call vector service for embedding
            float[] embedding = getEmbedding(combinedText);
            if (embedding == null || embedding.length == 0) {
                log.error("Failed to get embedding for task: {}", task);
                return false;
            }

            // 3. Update OpenSearch document with vector
            updateDocumentVector(task.getIndexName(), task.getDocumentId(),
                    task.getTargetField(), embedding);

            log.debug("Successfully vectorized document: index={}, id={}, field={}",
                    task.getIndexName(), task.getDocumentId(), task.getTargetField());
            return true;

        } catch (Exception e) {
            log.error("Failed to process vectorization task: {}", task, e);
            return false;
        }
    }

    /**
     * Get embedding from vector service
     *
     * @param text the input text
     * @return embedding vector
     */
    private float[] getEmbedding(String text) {
        try {
            String url = vectorServiceUrl + "/api/v1/embed";
            Map<String, String> request = new HashMap<>();
            request.put("text", text);

            Map<String, Object> response = restTemplate.postForObject(url, request, Map.class);

            if (response != null && response.containsKey("vector")) {
                @SuppressWarnings("unchecked")
                List<Number> vectorList = (List<Number>) response.get("vector");
                float[] vector = new float[vectorList.size()];
                for (int i = 0; i < vectorList.size(); i++) {
                    vector[i] = vectorList.get(i).floatValue();
                }
                return vector;
            }

        } catch (Exception e) {
            log.error("Failed to call vector service at {}", vectorServiceUrl, e);
        }
        return null;
    }

    /**
     * Update document with vector field using OpenSearch Update API
     *
     * @param indexName the index name
     * @param docId the document ID
     * @param vectorField the vector field name
     * @param vector the embedding vector
     */
    private void updateDocumentVector(String indexName, String docId,
                                     String vectorField, float[] vector) {
        try {
            // Build partial update document
            Map<String, Object> doc = new HashMap<>();
            doc.put(vectorField, vector);

            Map<String, Object> updateRequest = new HashMap<>();
            updateRequest.put("doc", doc);

            String requestBody = mapper.writeValueAsString(updateRequest);

            // Send POST update request
            String url = openSearchUrl + "/" + indexName + "/_update/" + docId;
            restTemplate.postForObject(url, requestBody, String.class);

        } catch (Exception e) {
            log.error("Failed to update document vector: index={}, id={}, field={}",
                    indexName, docId, vectorField, e);
            throw new RuntimeException("Failed to update document vector", e);
        }
    }

    /**
     * Check if vector service is available
     *
     * @return true if available
     */
    public boolean isVectorServiceAvailable() {
        try {
            String url = vectorServiceUrl + "/actuator/health";
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            return response != null && "UP".equals(response.get("status"));
        } catch (Exception e) {
            log.debug("Vector service not available at {}", vectorServiceUrl);
            return false;
        }
    }
}
