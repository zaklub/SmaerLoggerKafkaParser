package com.example.kafkaparsing.controller;

import com.example.kafkaparsing.service.DynamicMessageProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for testing dynamic message processing
 */
@RestController
@RequestMapping("/api/dynamic-message")
@CrossOrigin(origins = "*")
public class DynamicMessageTestController {

    @Autowired
    private DynamicMessageProcessor dynamicMessageProcessor;

    /**
     * Test dynamic message processing with sample JSON
     */
    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> testDynamicProcessing(@RequestBody String testMessage) {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Dynamic message processing test completed");
            response.put("input_message", testMessage);
            response.put("note", "Check logs for processing details");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Error testing dynamic processing: " + e.getMessage());
            return ResponseEntity.ok(response);
        }
    }

    /**
     * Get processor statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        try {
            String stats = dynamicMessageProcessor.getStatistics();
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("statistics", stats);
            response.put("message", "Dynamic message processor statistics");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Error getting statistics: " + e.getMessage());
            return ResponseEntity.ok(response);
        }
    }

    /**
     * Health check
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Dynamic Message Test Controller is running!");
        response.put("endpoints", new String[]{
            "POST /api/dynamic-message/test - Test dynamic message processing",
            "GET /api/dynamic-message/stats - Get processor statistics",
            "GET /api/dynamic-message/health - Health check"
        });
        
        return ResponseEntity.ok(response);
    }
}
