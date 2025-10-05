package com.example.kafkaparsing.controller;

import com.example.kafkaparsing.model.KafkaMessage;
import com.example.kafkaparsing.service.KafkaProducerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/kafka")
@CrossOrigin(origins = "*")
public class KafkaController {

    @Autowired
    private KafkaProducerService kafkaProducerService;

    /**
     * Send a simple message to Kafka
     */
    @PostMapping("/send-simple")
    public ResponseEntity<Map<String, String>> sendSimpleMessage(@RequestBody Map<String, String> request) {
        try {
            String message = request.get("message");
            if (message == null || message.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse("Message cannot be empty"));
            }

            kafkaProducerService.sendSimpleMessage(message);
            
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Message sent to Kafka successfully");
            response.put("topic", "notification-topic");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(createErrorResponse("Failed to send message: " + e.getMessage()));
        }
    }

    /**
     * Send a custom message to Kafka
     */
    @PostMapping("/send-custom")
    public ResponseEntity<Map<String, String>> sendCustomMessage(@RequestBody KafkaMessage kafkaMessage) {
        try {
            if (kafkaMessage.getMessage() == null || kafkaMessage.getMessage().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse("Message cannot be empty"));
            }

            kafkaProducerService.sendMessage(kafkaMessage);
            
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Custom message sent to Kafka successfully");
            response.put("topic", "notification-topic");
            response.put("messageId", kafkaMessage.getId());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(createErrorResponse("Failed to send custom message: " + e.getMessage()));
        }
    }

    /**
     * Send a message with all fields specified
     */
    @PostMapping("/send")
    public ResponseEntity<Map<String, String>> sendMessage(@RequestBody Map<String, String> request) {
        try {
            String id = request.get("id");
            String message = request.get("message");
            String source = request.get("source");
            String type = request.get("type");

            if (message == null || message.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse("Message cannot be empty"));
            }

            // Set defaults if not provided
            if (id == null) id = "auto-generated";
            if (source == null) source = "kafka-parsing-app";
            if (type == null) type = "user-message";

            kafkaProducerService.sendCustomMessage(id, message, source, type);
            
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Message sent to Kafka successfully");
            response.put("topic", "notification-topic");
            response.put("messageId", id);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(createErrorResponse("Failed to send message: " + e.getMessage()));
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Kafka Producer API is running!");
        response.put("topic", "notification-topic");
        response.put("bootstrap-servers", "localhost:9092");
        
        return ResponseEntity.ok(response);
    }

    private Map<String, String> createErrorResponse(String errorMessage) {
        Map<String, String> response = new HashMap<>();
        response.put("status", "error");
        response.put("message", errorMessage);
        return response;
    }
}


