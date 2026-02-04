package com.search.sync.vectorization;

import java.util.List;
import java.util.Map;

/**
 * Task for asynchronous vectorization of document fields
 *
 * Used in bypass mode: documents are indexed first,
 * then vectorized asynchronously to avoid blocking the main sync flow.
 */
public class VectorizationTask {

    /**
     * Target index name in OpenSearch
     */
    private final String indexName;

    /**
     * Document ID
     */
    private final String documentId;

    /**
     * Source field(s) to combine for vectorization
     * e.g., ["title", "description"]
     */
    private final List<String> sourceFields;

    /**
     * Target vector field name where the embedding will be stored
     * e.g., "combined_vector"
     */
    private final String targetField;

    /**
     * Raw document data for field extraction
     */
    private final Map<String, Object> documentData;

    /**
     * Task creation timestamp
     */
    private final long createdAt;

    /**
     * Retry count
     */
    private int retryCount = 0;

    /**
     * Maximum retries allowed
     */
    private static final int MAX_RETRIES = 3;

    public VectorizationTask(String indexName, String documentId,
                            List<String> sourceFields, String targetField,
                            Map<String, Object> documentData) {
        this.indexName = indexName;
        this.documentId = documentId;
        this.sourceFields = sourceFields;
        this.targetField = targetField;
        this.documentData = documentData;
        this.createdAt = System.currentTimeMillis();
    }

    public String getIndexName() {
        return indexName;
    }

    public String getDocumentId() {
        return documentId;
    }

    public List<String> getSourceFields() {
        return sourceFields;
    }

    public String getTargetField() {
        return targetField;
    }

    public Map<String, Object> getDocumentData() {
        return documentData;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void incrementRetry() {
        this.retryCount++;
    }

    public boolean shouldRetry() {
        return retryCount < MAX_RETRIES;
    }

    /**
     * Combine text from source fields for vectorization
     *
     * @return combined text string
     */
    public String combineText() {
        if (sourceFields == null || sourceFields.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (String field : sourceFields) {
            Object value = documentData.get(field);
            if (value != null) {
                String text = value.toString();
                if (!text.isEmpty()) {
                    if (sb.length() > 0) {
                        sb.append(" ");
                    }
                    sb.append(text);
                }
            }
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return "VectorizationTask{" +
                "indexName='" + indexName + '\'' +
                ", documentId='" + documentId + '\'' +
                ", sourceFields=" + sourceFields +
                ", targetField='" + targetField + '\'' +
                ", retryCount=" + retryCount +
                '}';
    }
}
