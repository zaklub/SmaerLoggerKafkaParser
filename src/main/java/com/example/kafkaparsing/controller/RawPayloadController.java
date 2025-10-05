package com.example.kafkaparsing.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/raw")
@CrossOrigin(origins = "*")
public class RawPayloadController {

    private static final Logger logger = LoggerFactory.getLogger(RawPayloadController.class);
    private static final String TOPIC_NAME = "raw-data-topic_kafka";

    @Autowired
    private KafkaTemplate<String, String> stringKafkaTemplate;

    /**
     * Raw Payload API - Accepts XML or JSON and pushes raw string to Kafka
     * 
     * JSON Example:
     * curl -X POST http://localhost:8080/api/raw/send \
     *   -H "Content-Type: application/json" \
     *   -d '{"message": "Hello from JSON"}'
     * 
     * XML Example:
     * curl -X POST http://localhost:8080/api/raw/send \
     *   -H "Content-Type: application/xml" \
     *   -d '<message>Hello from XML</message>'
     */
    @PostMapping("/send")
    public ResponseEntity<Map<String, Object>> sendRawPayload(HttpServletRequest request) {
        
        String messageId = UUID.randomUUID().toString();
        String contentType = request.getContentType();
        
        try {
            // Read raw body as string
            StringBuilder rawBody = new StringBuilder();
            BufferedReader reader = request.getReader();
            String line;
            while ((line = reader.readLine()) != null) {
                rawBody.append(line);
            }
            
            String payload = rawBody.toString();
            
            if (payload == null || payload.isEmpty()) {
                return ResponseEntity.badRequest().body(createResponse("error", "Empty payload", null, contentType));
            }
            
            logger.info("═══════════════════════════════════════════════════════");
            logger.info("📤 SENDING RAW PAYLOAD TO KAFKA");
            logger.info("═══════════════════════════════════════════════════════");
            logger.info("Message ID    : {}", messageId);
            logger.info("Content-Type  : {}", contentType);
            logger.info("Topic         : {}", TOPIC_NAME);
            logger.info("───────────────────────────────────────────────────────");
            logger.info("📄 RAW PAYLOAD:");
            logger.info("{}", payload);
            logger.info("═══════════════════════════════════════════════════════");
            
            // Send raw payload to Kafka
            stringKafkaTemplate.send(TOPIC_NAME, messageId, payload);
            
            logger.info("✅ Successfully sent raw payload to Kafka topic: {}", TOPIC_NAME);
            
            return ResponseEntity.ok(createResponse("success", "Raw payload sent to Kafka successfully", messageId, contentType));
            
        } catch (Exception e) {
            logger.error("❌ Error sending raw payload to Kafka: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createResponse("error", "Failed to send payload: " + e.getMessage(), messageId, contentType));
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Raw Payload API is running!");
        response.put("topic", TOPIC_NAME);
        return ResponseEntity.ok(response);
    }

    private Map<String, Object> createResponse(String status, String message, String messageId, String contentType) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", status);
        response.put("message", message);
        response.put("message_id", messageId);
        response.put("content_type", contentType);
        response.put("topic", TOPIC_NAME);
        return response;
    }
}



