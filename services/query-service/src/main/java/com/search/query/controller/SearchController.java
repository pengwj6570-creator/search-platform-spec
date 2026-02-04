package com.search.query.controller;

import com.search.query.model.SearchRequest;
import com.search.query.model.SearchResponse;
import com.search.query.service.SearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for search operations
 */
@RestController
@RequestMapping("/api/v1/search")
public class SearchController {

    private static final Logger log = LoggerFactory.getLogger(SearchController.class);

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    /**
     * Execute a search query
     *
     * POST /api/v1/search
     *
     * @param request the search request
     * @return search response with results
     */
    @PostMapping
    public ResponseEntity<SearchResponse> search(@RequestBody SearchRequest request) {
        try {
            // Validate request
            if (request.getQuery() == null && (request.getFilters() == null || request.getFilters().isEmpty())) {
                return ResponseEntity.badRequest().build();
            }

            SearchResponse response = searchService.search(request);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Search request failed", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Health check endpoint
     *
     * GET /api/v1/search/health
     *
     * @return health status
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> status = new HashMap<>();
        status.put("status", "UP");
        status.put("service", "query-service");
        return ResponseEntity.ok(status);
    }
}
