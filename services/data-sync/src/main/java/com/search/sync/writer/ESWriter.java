package com.search.sync.writer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.search.sync.config.OpenSearchConfig;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.Refresh;
import org.opensearch.client.opensearch.core.IndexRequest;
import org.opensearch.client.opensearch.core.DeleteRequest;
import org.opensearch.client.opensearch.core.IndexResponse;
import org.opensearch.client.opensearch.core.DeleteResponse;
import org.opensearch.client.opensearch.indices.CreateIndexRequest;
import org.opensearch.client.opensearch.core.CreateResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.StringReader;
import java.util.Map;

/**
 * Writer for synchronizing data to OpenSearch
 *
 * Handles upsert (create/update) and delete operations on OpenSearch indices.
 * Automatically creates indices if they don't exist.
 */
@Component
public class ESWriter {

    private static final Logger log = LoggerFactory.getLogger(ESWriter.class);

    private final OpenSearchClient client;
    private final String indexPrefix;
    private final ObjectMapper mapper;

    /**
     * Create a new ES writer
     *
     * @param client OpenSearch client
     * @param config OpenSearch configuration
     */
    public ESWriter(OpenSearchClient client, OpenSearchConfig config) {
        this.client = client;
        this.indexPrefix = config.getIndexPrefix() != null ? config.getIndexPrefix() : "search";
        this.mapper = new ObjectMapper();
    }

    /**
     * Upsert a document (create or update)
     *
     * @param objectType the object type (used as suffix for index name)
     * @param id document ID
     * @param document JSON document
     */
    public void upsert(String objectType, String id, String document) {
        if (objectType == null || id == null || document == null) {
            throw new IllegalArgumentException("objectType, id, and document must not be null");
        }

        String indexName = getIndexName(objectType);

        try {
            // Ensure index exists
            ensureIndexExists(indexName);

            // Parse JSON string to Map for proper serialization
            @SuppressWarnings("unchecked")
            Map<String, Object> docMap = mapper.readValue(document, Map.class);

            // For upsert, we'll use index with a simple approach
            // If the document already exists, index will update it with version increment
            IndexRequest<Map<String, Object>> indexRequest = IndexRequest.of(
                    i -> i.index(indexName)
                            .id(id)
                            .document(docMap)
                            .refresh(Refresh.True)
            );

            IndexResponse indexResponse = client.index(indexRequest);

            log.debug("Upserted document: index={}, id={}, result={}",
                    indexName, id, indexResponse.result());

        } catch (Exception e) {
            log.error("Failed to upsert document: index={}, id={}", indexName, id, e);
            throw new RuntimeException("Failed to upsert document", e);
        }
    }

    /**
     * Delete a document
     *
     * @param objectType the object type (used as suffix for index name)
     * @param id document ID
     */
    public void delete(String objectType, String id) {
        if (objectType == null || id == null) {
            throw new IllegalArgumentException("objectType and id must not be null");
        }

        String indexName = getIndexName(objectType);

        try {
            DeleteRequest deleteRequest = DeleteRequest.of(
                    d -> d.index(indexName)
                            .id(id)
                            .refresh(Refresh.True)
            );

            DeleteResponse response = client.delete(deleteRequest);

            log.debug("Deleted document: index={}, id={}, result={}",
                    indexName, id, response.result());

        } catch (Exception e) {
            // Ignore 404 errors (document not found)
            if (e.getMessage() != null && e.getMessage().contains("index_not_found_exception")) {
                log.debug("Index not found, ignoring delete: index={}, id={}", indexName, id);
                return;
            }
            log.error("Failed to delete document: index={}, id={}", indexName, id, e);
            throw new RuntimeException("Failed to delete document", e);
        }
    }

    /**
     * Get the full index name for an object type
     *
     * @param objectType the object type
     * @return full index name
     */
    private String getIndexName(String objectType) {
        return indexPrefix + "_" + objectType.toLowerCase();
    }

    /**
     * Ensure the index exists, create if not
     *
     * @param indexName the index name
     */
    private void ensureIndexExists(String indexName) {
        try {
            boolean exists = client.indices().exists(e -> e.index(indexName)).value();

            if (!exists) {
                log.info("Creating index: {}", indexName);

                // Use minimal default mapping - OpenSearch will infer types from data
                CreateIndexRequest createRequest = CreateIndexRequest.of(
                        c -> c.index(indexName)
                );

                client.indices().create(createRequest);
                log.info("Index created: {}", indexName);
            }

        } catch (Exception e) {
            log.warn("Failed to check/create index: {}", indexName, e);
            // Continue anyway, as the index might have been created by another thread
        }
    }

    /**
     * Bulk upsert multiple documents
     *
     * @param objectType the object type
     * @param documents map of document ID to JSON document
     */
    public void bulkUpsert(String objectType, Map<String, String> documents) {
        for (Map.Entry<String, String> entry : documents.entrySet()) {
            try {
                upsert(objectType, entry.getKey(), entry.getValue());
            } catch (Exception e) {
                log.error("Failed to upsert document in bulk: id={}", entry.getKey(), e);
            }
        }
        log.info("Bulk upsert completed: objectType={}, count={}", objectType, documents.size());
    }
}
