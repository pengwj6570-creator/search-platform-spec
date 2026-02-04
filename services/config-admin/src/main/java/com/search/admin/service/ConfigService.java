package com.search.admin.service;

import com.search.config.model.SearchObject;
import com.search.config.model.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Configuration service for managing search metadata
 *
 * Uses in-memory storage for simplicity. In production, this would
 * be backed by a persistent database.
 */
@Service
public class ConfigService {

    private static final Logger log = LoggerFactory.getLogger(ConfigService.class);

    private final Map<String, Source> sources = new ConcurrentHashMap<>();
    private final Map<String, SearchObject> objects = new ConcurrentHashMap<>();

    // ========== Source Operations ==========

    /**
     * Create a new data source configuration
     *
     * @param source the source configuration
     * @return the created source
     * @throws IllegalArgumentException if source ID already exists
     */
    public Source createSource(Source source) {
        if (source == null) {
            throw new IllegalArgumentException("Source cannot be null");
        }
        if (source.getSourceId() == null || source.getSourceId().isEmpty()) {
            throw new IllegalArgumentException("Source ID cannot be null or empty");
        }
        if (sources.containsKey(source.getSourceId())) {
            throw new IllegalArgumentException("Source with ID " + source.getSourceId() + " already exists");
        }

        sources.put(source.getSourceId(), source);
        log.info("Created source: {}", source.getSourceId());
        return source;
    }

    /**
     * Get a source by ID
     *
     * @param sourceId the source ID
     * @return the source, or null if not found
     */
    public Source getSource(String sourceId) {
        return sources.get(sourceId);
    }

    /**
     * Get all sources
     *
     * @return collection of all sources
     */
    public Collection<Source> getAllSources() {
        return Collections.unmodifiableCollection(sources.values());
    }

    /**
     * Update an existing source
     *
     * @param sourceId the source ID
     * @param source the updated source configuration
     * @return the updated source
     * @throws IllegalArgumentException if source not found
     */
    public Source updateSource(String sourceId, Source source) {
        if (!sources.containsKey(sourceId)) {
            throw new IllegalArgumentException("Source not found: " + sourceId);
        }
        source.setSourceId(sourceId);
        sources.put(sourceId, source);
        log.info("Updated source: {}", sourceId);
        return source;
    }

    /**
     * Delete a source
     *
     * @param sourceId the source ID
     * @return true if deleted, false if not found
     */
    public boolean deleteSource(String sourceId) {
        Source removed = sources.remove(sourceId);
        if (removed != null) {
            log.info("Deleted source: {}", sourceId);
            return true;
        }
        return false;
    }

    // ========== SearchObject Operations ==========

    /**
     * Create a new search object configuration
     *
     * @param object the search object configuration
     * @return the created search object
     * @throws IllegalArgumentException if object ID already exists
     */
    public SearchObject createObject(SearchObject object) {
        if (object == null) {
            throw new IllegalArgumentException("SearchObject cannot be null");
        }
        if (object.getObjectId() == null || object.getObjectId().isEmpty()) {
            throw new IllegalArgumentException("Object ID cannot be null or empty");
        }
        if (objects.containsKey(object.getObjectId())) {
            throw new IllegalArgumentException("SearchObject with ID " + object.getObjectId() + " already exists");
        }

        // Validate source reference
        if (object.getSourceId() != null && !sources.containsKey(object.getSourceId())) {
            log.warn("Creating object {} with non-existent source reference: {}",
                    object.getObjectId(), object.getSourceId());
        }

        objects.put(object.getObjectId(), object);
        log.info("Created search object: {}", object.getObjectId());
        return object;
    }

    /**
     * Get a search object by ID
     *
     * @param objectId the object ID
     * @return the search object, or null if not found
     */
    public SearchObject getObject(String objectId) {
        return objects.get(objectId);
    }

    /**
     * Get all search objects
     *
     * @return collection of all search objects
     */
    public Collection<SearchObject> getAllObjects() {
        return Collections.unmodifiableCollection(objects.values());
    }

    /**
     * Get all search objects for a specific app key
     *
     * @param appKey the application key
     * @return collection of search objects for the app
     */
    public Collection<SearchObject> getObjectsByAppKey(String appKey) {
        return objects.values().stream()
                .filter(obj -> appKey.equals(obj.getAppKey()))
                .toList();
    }

    /**
     * Update an existing search object
     *
     * @param objectId the object ID
     * @param object the updated search object configuration
     * @return the updated search object
     * @throws IllegalArgumentException if object not found
     */
    public SearchObject updateObject(String objectId, SearchObject object) {
        if (!objects.containsKey(objectId)) {
            throw new IllegalArgumentException("SearchObject not found: " + objectId);
        }
        object.setObjectId(objectId);
        objects.put(objectId, object);
        log.info("Updated search object: {}", objectId);
        return object;
    }

    /**
     * Delete a search object
     *
     * @param objectId the object ID
     * @return true if deleted, false if not found
     */
    public boolean deleteObject(String objectId) {
        SearchObject removed = objects.remove(objectId);
        if (removed != null) {
            log.info("Deleted search object: {}", objectId);
            return true;
        }
        return false;
    }
}
