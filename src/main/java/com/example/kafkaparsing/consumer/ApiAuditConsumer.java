package com.example.kafkaparsing.consumer;

import com.example.kafkaparsing.model.ApiAuditLog;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
public class ApiAuditConsumer {

    private static final Logger logger = LoggerFactory.getLogger(ApiAuditConsumer.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String RAW_DATA_TOPIC = "raw-data-topic_kafka";

    @Autowired
    private KafkaTemplate<String, String> stringKafkaTemplate;

    @KafkaListener(topics = "api_audit_zak_logs", groupId = "api-audit-consumer-group")
    public void consumeApiAuditLog(@Payload String message,
                                 @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                 @Header(KafkaHeaders.RECEIVED_PARTITION_ID) int partition,
                                 @Header(KafkaHeaders.OFFSET) long offset,
                                 @Header(value = KafkaHeaders.RECEIVED_MESSAGE_KEY, required = false) String key) {
        
        try {
            logger.info("════════════════════════════════════════════════════════════");
            logger.info("📨 API AUDIT LOG MESSAGE RECEIVED");
            logger.info("════════════════════════════════════════════════════════════");
            logger.info("Source Topic : {}", topic);
            logger.info("Partition    : {}", partition);
            logger.info("Offset       : {}", offset);
            logger.info("Key          : {}", key != null ? key : "null");
            logger.info("────────────────────────────────────────────────────────────");
            
            // Parse the message to ApiAuditLog
            ApiAuditLog auditLog = objectMapper.readValue(message, ApiAuditLog.class);
            
            // Print formatted audit log details
            printAuditLogDetails(auditLog);
            
            // Forward the raw message to raw-data-topic_kafka
            forwardToRawDataTopic(message, key, auditLog);
            
            logger.info("════════════════════════════════════════════════════════════");
            logger.info("");
            
        } catch (Exception e) {
            logger.error("❌ Error processing Kafka message from topic {}: {}", topic, e.getMessage(), e);
            logger.error("Raw message that failed to parse: {}", message);
        }
    }

    /**
     * Print formatted audit log details to console
     */
    private void printAuditLogDetails(ApiAuditLog auditLog) {
        logger.info("┌─────────────────────────────────────────────────────────────┐");
        logger.info("│                    API AUDIT LOG                            │");
        logger.info("├─────────────────────────────────────────────────────────────┤");
        logger.info("│ Log ID: {}", String.format("%-45s", auditLog.getLogId()));
        logger.info("│ API Name: {}", String.format("%-42s", auditLog.getApiName()));
        logger.info("│ Request ID: {}", String.format("%-40s", auditLog.getRequestId()));
        logger.info("│ Log Type: {}", String.format("%-42s", auditLog.getLogType()));
        logger.info("│ Timestamp: {}", String.format("%-40s", auditLog.getTimestamp()));
        logger.info("├─────────────────────────────────────────────────────────────┤");
        
        if (auditLog.getMetadata() != null) {
            logger.info("│                        METADATA                          │");
            logger.info("├─────────────────────────────────────────────────────────────┤");
            logger.info("│ Client IP: {}", String.format("%-40s", auditLog.getMetadata().getClientIp()));
            logger.info("│ HTTP Method: {}", String.format("%-37s", auditLog.getMetadata().getHttpMethod()));
            logger.info("│ Endpoint: {}", String.format("%-42s", auditLog.getMetadata().getEndpoint()));
            logger.info("│ Content Type: {}", String.format("%-36s", auditLog.getMetadata().getContentType()));
            logger.info("│ Content Length: {}", String.format("%-33s", auditLog.getMetadata().getContentLength()));
            
            if (auditLog.getMetadata().getResponseStatus() != null) {
                logger.info("│ Response Status: {}", String.format("%-32s", auditLog.getMetadata().getResponseStatus()));
            }
            
            if (auditLog.getMetadata().getProcessingTimeMs() != null) {
                logger.info("│ Processing Time: {} ms", String.format("%-30s", auditLog.getMetadata().getProcessingTimeMs()));
            }
            
            if (auditLog.getMetadata().getUserAgent() != null) {
                logger.info("│ User Agent: {}", String.format("%-40s", 
                    auditLog.getMetadata().getUserAgent().length() > 40 ? 
                    auditLog.getMetadata().getUserAgent().substring(0, 37) + "..." : 
                    auditLog.getMetadata().getUserAgent()));
            }
        }
        
        logger.info("├─────────────────────────────────────────────────────────────┤");
        logger.info("│                        PAYLOAD                             │");
        logger.info("├─────────────────────────────────────────────────────────────┤");
        
        try {
            String payloadJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(auditLog.getPayload());
            String[] payloadLines = payloadJson.split("\n");
            for (String line : payloadLines) {
                if (line.length() > 60) {
                    logger.info("│ {}", String.format("%-58s", line.substring(0, 55) + "..."));
                } else {
                    logger.info("│ {}", String.format("%-58s", line));
                }
            }
        } catch (Exception e) {
            logger.info("│ {}", String.format("%-58s", "Error formatting payload: " + e.getMessage()));
        }
        
        logger.info("└─────────────────────────────────────────────────────────────┘");
    }

    /**
     * Forward the raw message to raw-data-topic_kafka
     */
    private void forwardToRawDataTopic(String message, String key, ApiAuditLog auditLog) {
        try {
            // Use the audit log's request ID as the key, or fall back to the original key
            String forwardKey = auditLog.getRequestId() != null ? auditLog.getRequestId() : key;
            
            logger.info("📤 FORWARDING TO RAW DATA TOPIC");
            logger.info("Target Topic : {}", RAW_DATA_TOPIC);
            logger.info("Key          : {}", forwardKey);
            logger.info("Log Type     : {}", auditLog.getLogType());
            logger.info("API Name     : {}", auditLog.getApiName());
            
            // Send the raw message to raw-data-topic_kafka
            stringKafkaTemplate.send(RAW_DATA_TOPIC, forwardKey, message);
            
            logger.info("✅ Successfully forwarded to {}", RAW_DATA_TOPIC);
            
        } catch (Exception e) {
            logger.error("❌ Failed to forward message to {}: {}", RAW_DATA_TOPIC, e.getMessage(), e);
        }
    }
}
