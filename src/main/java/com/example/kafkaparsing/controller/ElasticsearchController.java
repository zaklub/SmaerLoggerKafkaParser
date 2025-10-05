package com.example.kafkaparsing.controller;

import com.example.kafkaparsing.service.ElasticsearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/elasticsearch")
@CrossOrigin(origins = "*")
public class ElasticsearchController {

    @Autowired
    private ElasticsearchService elasticsearchService;

    /**
     * Get all indices from Elasticsearch
     */
    @GetMapping("/indices")
    public ResponseEntity<Map<String, Object>> getAllIndices() {
        try {
            List<String> indices = elasticsearchService.getAllIndices();
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("count", indices.size());
            response.put("indices", indices);
            response.put("host", "localhost:9200");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(createErrorResponse("Failed to retrieve indices: " + e.getMessage()));
        }
    }

    /**
     * Get detailed information about all indices
     */
    @GetMapping("/indices/details")
    public ResponseEntity<Map<String, Object>> getIndicesDetails() {
        try {
            Map<String, Object> details = elasticsearchService.getIndicesDetails();
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("details", details);
            response.put("host", "localhost:9200");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(createErrorResponse("Failed to retrieve indices details: " + e.getMessage()));
        }
    }

    /**
     * Get cluster health information
     */
    @GetMapping("/cluster/health")
    public ResponseEntity<Map<String, Object>> getClusterHealth() {
        try {
            Map<String, Object> health = elasticsearchService.getClusterHealth();
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("health", health);
            response.put("host", "localhost:9200");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(createErrorResponse("Failed to retrieve cluster health: " + e.getMessage()));
        }
    }

    /**
     * Check if Elasticsearch is available
     */
    @GetMapping("/ping")
    public ResponseEntity<Map<String, Object>> ping() {
        try {
            boolean isAvailable = elasticsearchService.isElasticsearchAvailable();
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("available", isAvailable);
            response.put("host", "localhost:9200");
            response.put("message", isAvailable ? "Elasticsearch is available" : "Elasticsearch is not available");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(createErrorResponse("Failed to ping Elasticsearch: " + e.getMessage()));
        }
    }

    /**
     * Get index templates
     */
    @GetMapping("/templates")
    public ResponseEntity<Map<String, Object>> getIndexTemplates() {
        try {
            List<String> templates = elasticsearchService.getIndexTemplates();
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("count", templates.size());
            response.put("templates", templates);
            response.put("host", "localhost:9200");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(createErrorResponse("Failed to retrieve index templates: " + e.getMessage()));
        }
    }

    /**
     * Get mappings for a specific index
     */
    @GetMapping("/indices/{indexName}/mappings")
    public ResponseEntity<Map<String, Object>> getIndexMappings(@PathVariable String indexName) {
        try {
            Map<String, Object> mappings = elasticsearchService.getIndexMappings(indexName);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("index", indexName);
            response.put("mappings", mappings);
            response.put("host", "localhost:9200");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(createErrorResponse("Failed to retrieve mappings for index " + indexName + ": " + e.getMessage()));
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Elasticsearch API is running!");
        response.put("host", "localhost:9200");
        response.put("endpoints", List.of(
            "GET /api/elasticsearch/indices - Get all indices",
            "GET /api/elasticsearch/indices/details - Get detailed indices info",
            "GET /api/elasticsearch/indices/{indexName}/mappings - Get mappings for an index",
            "GET /api/elasticsearch/cluster/health - Get cluster health",
            "GET /api/elasticsearch/ping - Ping Elasticsearch",
            "GET /api/elasticsearch/templates - Get index templates"
        ));
        
        return ResponseEntity.ok(response);
    }

    private Map<String, Object> createErrorResponse(String errorMessage) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "error");
        response.put("message", errorMessage);
        response.put("host", "localhost:9200");
        return response;
    }
}
