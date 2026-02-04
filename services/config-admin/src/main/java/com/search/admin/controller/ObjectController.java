package com.search.admin.controller;

import com.search.config.model.SearchObject;
import com.search.admin.service.ConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

/**
 * REST Controller for managing search object configurations
 */
@RestController
@RequestMapping("/api/v1/objects")
public class ObjectController {

    private static final Logger log = LoggerFactory.getLogger(ObjectController.class);

    private final ConfigService configService;

    public ObjectController(ConfigService configService) {
        this.configService = configService;
    }

    /**
     * Create a new search object
     *
     * POST /api/v1/objects
     */
    @PostMapping
    public ResponseEntity<SearchObject> createObject(@RequestBody SearchObject object) {
        try {
            SearchObject created = configService.createObject(object);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            log.warn("Failed to create search object: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get a search object by ID
     *
     * GET /api/v1/objects/{objectId}
     */
    @GetMapping("/{objectId}")
    public ResponseEntity<SearchObject> getObject(@PathVariable String objectId) {
        SearchObject object = configService.getObject(objectId);
        if (object == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(object);
    }

    /**
     * List all search objects
     *
     * GET /api/v1/objects
     */
    @GetMapping
    public ResponseEntity<Collection<SearchObject>> listObjects(
            @RequestParam(required = false) String appKey) {
        if (appKey != null && !appKey.isEmpty()) {
            return ResponseEntity.ok(configService.getObjectsByAppKey(appKey));
        }
        return ResponseEntity.ok(configService.getAllObjects());
    }

    /**
     * Update a search object
     *
     * PUT /api/v1/objects/{objectId}
     */
    @PutMapping("/{objectId}")
    public ResponseEntity<SearchObject> updateObject(
            @PathVariable String objectId,
            @RequestBody SearchObject object) {
        try {
            SearchObject updated = configService.updateObject(objectId, object);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            log.warn("Failed to update search object: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Delete a search object
     *
     * DELETE /api/v1/objects/{objectId}
     */
    @DeleteMapping("/{objectId}")
    public ResponseEntity<Void> deleteObject(@PathVariable String objectId) {
        boolean deleted = configService.deleteObject(objectId);
        if (!deleted) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }
}
