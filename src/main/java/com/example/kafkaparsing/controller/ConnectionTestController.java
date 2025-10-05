package com.example.kafkaparsing.controller;

import com.example.kafkaparsing.model.KafkaConnectionDetails;
import com.example.kafkaparsing.service.DataSourceConnectionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/connection-test")
@CrossOrigin(origins = "*")
public class ConnectionTestController {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionTestController.class);

    @Autowired
    private DataSourceConnectionService dataSourceConnectionService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Test parsing the new JSON structure
     */
    @PostMapping("/parse-new-json")
    public ResponseEntity<Map<String, Object>> parseNewJson(@RequestBody String jsonString) {
        try {
            logger.info("Testing new JSON structure parsing...");
            logger.info("Input JSON: {}", jsonString);

            // Parse using the updated KafkaConnectionDetails
            KafkaConnectionDetails details = dataSourceConnectionService.parseConnectionDetails(jsonString);
            
            // Validate
            boolean isValid = dataSourceConnectionService.validateConnectionDetails(details);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("parsed", details);
            response.put("valid", isValid);
            response.put("message", isValid ? "JSON parsed and validated successfully" : "JSON parsed but validation failed");

            logger.info("Parsing result: {}", response);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error parsing new JSON structure: {}", e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Failed to parse JSON: " + e.getMessage());
            response.put("input", jsonString);
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Test with the exact JSON structure you provided
     */
    @GetMapping("/test-example")
    public ResponseEntity<Map<String, Object>> testExample() {
        String exampleJson = "{\n" +
                "  \"kafkaBrokers\": [\n" +
                "    \"localhost:9092\"\n" +
                "  ],\n" +
                "  \"connectionName\": \"Kafka Frontend\",\n" +
                "  \"topic\": \"api_audit_zak_logs\",\n" +
                "  \"consumerGroupId\": \"local-dev-consumer\",\n" +
                "  \"securityProtocol\": \"PLAINTEXT\",\n" +
                "  \"certificate\": null,\n" +
                "  \"userName\": \"\",\n" +
                "  \"password\": \"\"\n" +
                "}";

        return parseNewJson(exampleJson);
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Connection Test API is running!");
        response.put("endpoints", new String[]{
            "POST /api/connection-test/parse-new-json - Test JSON parsing",
            "GET /api/connection-test/test-example - Test with example JSON",
            "GET /api/connection-test/health - Health check"
        });
        return ResponseEntity.ok(response);
    }
}


