package com.example.kafkaparsing.service;

import com.example.kafkaparsing.model.ApiAuditLog;
import com.example.kafkaparsing.model.ApiMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class ApiAuditService {

    private static final Logger logger = LoggerFactory.getLogger(ApiAuditService.class);
    private static final String TOPIC_NAME = "api_audit_zak_logs";

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Send request audit log to Kafka
     */
    public void sendRequestAuditLog(String apiName, String requestId, Object payload, HttpServletRequest request) {
        try {
            ApiMetadata metadata = createRequestMetadata(request);
            ApiAuditLog auditLog = new ApiAuditLog(apiName, requestId, "REQUEST", metadata, payload);
            
            logger.info("Sending REQUEST audit log to Kafka topic {}: {}", TOPIC_NAME, auditLog);
            kafkaTemplate.send(TOPIC_NAME, requestId, auditLog);
            
        } catch (Exception e) {
            logger.error("Failed to send REQUEST audit log to Kafka", e);
        }
    }

    /**
     * Send response audit log to Kafka
     */
    public void sendResponseAuditLog(String apiName, String requestId, Object payload, 
                                   HttpServletRequest request, Integer responseStatus, Long processingTimeMs) {
        try {
            ApiMetadata metadata = createResponseMetadata(request, responseStatus, processingTimeMs);
            ApiAuditLog auditLog = new ApiAuditLog(apiName, requestId, "RESPONSE", metadata, payload);
            
            logger.info("Sending RESPONSE audit log to Kafka topic {}: {}", TOPIC_NAME, auditLog);
            kafkaTemplate.send(TOPIC_NAME, requestId, auditLog);
            
        } catch (Exception e) {
            logger.error("Failed to send RESPONSE audit log to Kafka", e);
        }
    }

    /**
     * Create request metadata from HttpServletRequest
     */
    private ApiMetadata createRequestMetadata(HttpServletRequest request) {
        return new ApiMetadata(
            getClientIpAddress(request),
            request.getHeader("User-Agent"),
            request.getContentType(),
            request.getContentLengthLong(),
            request.getMethod(),
            request.getRequestURI(),
            null, // No response status for request
            null  // No processing time for request
        );
    }

    /**
     * Create response metadata from HttpServletRequest and response details
     */
    private ApiMetadata createResponseMetadata(HttpServletRequest request, Integer responseStatus, Long processingTimeMs) {
        return new ApiMetadata(
            getClientIpAddress(request),
            request.getHeader("User-Agent"),
            request.getContentType(),
            request.getContentLengthLong(),
            request.getMethod(),
            request.getRequestURI(),
            responseStatus,
            processingTimeMs
        );
    }

    /**
     * Get client IP address from request
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }

    /**
     * Generate unique request ID
     */
    public String generateRequestId() {
        return UUID.randomUUID().toString();
    }
}


