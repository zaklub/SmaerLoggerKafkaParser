package com.example.kafkaparsing.controller;

import com.example.kafkaparsing.service.DynamicMessageProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for Audit Data Processor operations
 */
@RestController
@RequestMapping("/api/audit-processor")
@CrossOrigin(origins = "*")
public class AuditProcessorController {

    @Autowired
    private DynamicMessageProcessor dynamicMessageProcessor;

    /**
     * Get audit processor statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        try {
            String stats = dynamicMessageProcessor.getStatistics();
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("statistics", stats);
            response.put("message", "Audit processor is running");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Error getting audit processor statistics: " + e.getMessage());
            return ResponseEntity.ok(response);
        }
    }

    /**
     * Health check for audit processor
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Dynamic Message Processor is running!");
        response.put("endpoints", new String[]{
            "GET /api/audit-processor/stats - Get processor statistics",
            "GET /api/audit-processor/health - Health check"
        });
        
        return ResponseEntity.ok(response);
    }
}

