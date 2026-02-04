package com.search.admin.controller;

import com.search.config.model.Source;
import com.search.admin.service.ConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.UUID;

/**
 * REST Controller for managing data source configurations
 */
@RestController
@RequestMapping("/api/v1/sources")
public class SourceController {

    private static final Logger log = LoggerFactory.getLogger(SourceController.class);

    private final ConfigService configService;

    public SourceController(ConfigService configService) {
        this.configService = configService;
    }

    /**
     * Create a new data source
     *
     * POST /api/v1/sources
     */
    @PostMapping
    public ResponseEntity<Source> createSource(@RequestBody Source source) {
        try {
            Source created = configService.createSource(source);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            log.warn("Failed to create source: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get a data source by ID
     *
     * GET /api/v1/sources/{sourceId}
     */
    @GetMapping("/{sourceId}")
    public ResponseEntity<Source> getSource(@PathVariable String sourceId) {
        Source source = configService.getSource(sourceId);
        if (source == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(source);
    }

    /**
     * List all data sources
     *
     * GET /api/v1/sources
     */
    @GetMapping
    public ResponseEntity<Collection<Source>> listSources() {
        return ResponseEntity.ok(configService.getAllSources());
    }

    /**
     * Update a data source
     *
     * PUT /api/v1/sources/{sourceId}
     */
    @PutMapping("/{sourceId}")
    public ResponseEntity<Source> updateSource(
            @PathVariable String sourceId,
            @RequestBody Source source) {
        try {
            Source updated = configService.updateSource(sourceId, source);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            log.warn("Failed to update source: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Delete a data source
     *
     * DELETE /api/v1/sources/{sourceId}
     */
    @DeleteMapping("/{sourceId}")
    public ResponseEntity<Void> deleteSource(@PathVariable String sourceId) {
        boolean deleted = configService.deleteSource(sourceId);
        if (!deleted) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }
}
