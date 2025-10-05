package com.example.kafkaparsing.service;

import com.example.kafkaparsing.model.ApiAuditLog;
import com.example.kafkaparsing.model.ParsedAuditData;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service responsible for processing audit data from RawDataConsumer
 * Handles Request/Response correlation and sends data to Elasticsearch
 */
@Service
public class AuditDataProcessor {

    private static final Logger logger = LoggerFactory.getLogger(AuditDataProcessor.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @Value("${audit.processor.timeout-minutes:1}")
    private int timeoutMinutes;

    @Value("${audit.processor.cleanup-interval-minutes:5}")
    private int cleanupIntervalMinutes;

    @Autowired
    private ElasticsearchService elasticsearchService;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private final ConcurrentHashMap<String, ParsedAuditData> pendingRequests = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ParsedAuditData> completedTransactions = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newFixedThreadPool(5);

    @PostConstruct
    public void init() {
        logger.info("🔧 AuditDataProcessor initialized with timeout: {} minutes", timeoutMinutes);
    }

    @PreDestroy
    public void shutdown() {
        logger.info("🛑 Shutting down AuditDataProcessor");
        executorService.shutdown();
    }

    /**
     * Process incoming audit log message
     * Handles both REQUEST and RESPONSE messages
     */
    public void processAuditMessage(String message) {
        try {
            logger.debug("📨 Processing audit message: {}", message.length() > 100 ? 
                message.substring(0, 100) + "..." : message);

            ApiAuditLog auditLog = objectMapper.readValue(message, ApiAuditLog.class);
            
            if ("REQUEST".equals(auditLog.getLogType())) {
                processRequest(auditLog);
            } else if ("RESPONSE".equals(auditLog.getLogType())) {
                processResponse(auditLog);
            } else {
                logger.warn("⚠️ Unknown log type: {}", auditLog.getLogType());
            }

        } catch (Exception e) {
            logger.error("❌ Error processing audit message: {}", e.getMessage(), e);
        }
    }

    /**
     * Process REQUEST message
     * Store in pending requests and schedule timeout
     */
    private void processRequest(ApiAuditLog requestLog) {
        String requestId = requestLog.getRequestId();
        
        logger.info("📝 Processing REQUEST - ID: {}, API: {}", 
            requestId, requestLog.getApiName());

        ParsedAuditData parsedData = createParsedDataFromRequest(requestLog);
        pendingRequests.put(requestId, parsedData);

        // Schedule timeout processing
        scheduleTimeoutProcessing(requestId);

        logger.info("⏳ REQUEST stored, waiting for RESPONSE. Pending requests: {}", 
            pendingRequests.size());
    }

    /**
     * Process RESPONSE message
     * Look for corresponding REQUEST and complete the transaction
     */
    private void processResponse(ApiAuditLog responseLog) {
        String requestId = responseLog.getRequestId();
        
        logger.info("📤 Processing RESPONSE - ID: {}, API: {}", 
            requestId, responseLog.getApiName());

        ParsedAuditData pendingData = pendingRequests.get(requestId);
        
        if (pendingData != null) {
            // Found matching request, complete the transaction
            completeTransaction(pendingData, responseLog);
            pendingRequests.remove(requestId);
            
            logger.info("✅ Transaction completed for ID: {}. Remaining pending: {}", 
                requestId, pendingRequests.size());
        } else {
            // Orphaned response - wait for request
            logger.info("🔄 Orphaned RESPONSE detected for ID: {}. Waiting for REQUEST...", requestId);
            handleOrphanedResponse(responseLog);
        }
    }

    /**
     * Handle orphaned response (response without matching request)
     * Store and wait for corresponding request
     */
    private void handleOrphanedResponse(ApiAuditLog responseLog) {
        String requestId = responseLog.getRequestId();
        
        ParsedAuditData orphanedData = new ParsedAuditData(requestId);
        updateParsedDataFromResponse(orphanedData, responseLog);
        orphanedData.setIsComplete(false);
        
        completedTransactions.put(requestId, orphanedData);
        
        // Schedule timeout for orphaned response
        scheduleTimeoutProcessing(requestId + "_orphaned");
        
        logger.info("🔄 Orphaned RESPONSE stored for ID: {}. Waiting for REQUEST...", requestId);
    }

    /**
     * Complete transaction by merging request and response data
     */
    private void completeTransaction(ParsedAuditData parsedData, ApiAuditLog responseLog) {
        updateParsedDataFromResponse(parsedData, responseLog);
        parsedData.setIsComplete(true);
        
        // Send to Elasticsearch
        sendToElasticsearch(parsedData);
        
        logger.info("🎯 Transaction completed and indexed: {}", parsedData.getCorrelationId());
    }

    /**
     * Create ParsedAuditData from REQUEST message
     */
    private ParsedAuditData createParsedDataFromRequest(ApiAuditLog requestLog) {
        ParsedAuditData parsedData = new ParsedAuditData(requestLog.getRequestId());
        
        // Map request fields
        parsedData.setApiName(requestLog.getApiName());
        parsedData.setHost(requestLog.getMetadata().getClientIp());
        parsedData.setResourcePath(requestLog.getMetadata().getEndpoint());
        
        // Extract request payload
        try {
            parsedData.setRequestPayload(objectMapper.writeValueAsString(requestLog.getPayload()));
        } catch (Exception e) {
            logger.warn("⚠️ Failed to serialize request payload: {}", e.getMessage());
            parsedData.setRequestPayload("{}");
        }
        
        // Extract request time from payload
        parsedData.setRequestTime(extractTimeFromPayload(requestLog.getPayload()));
        
        logger.debug("📝 Created ParsedAuditData from REQUEST: {}", parsedData.getCorrelationId());
        return parsedData;
    }

    /**
     * Update ParsedAuditData with RESPONSE message data
     */
    private void updateParsedDataFromResponse(ParsedAuditData parsedData, ApiAuditLog responseLog) {
        // Extract response payload
        try {
            parsedData.setResponsePayload(objectMapper.writeValueAsString(responseLog.getPayload()));
        } catch (Exception e) {
            logger.warn("⚠️ Failed to serialize response payload: {}", e.getMessage());
            parsedData.setResponsePayload("{}");
        }
        
        // Extract response time from payload
        parsedData.setResponseTime(extractTimeFromPayload(responseLog.getPayload()));
        
        // Extract status and status code
        parsedData.setStatus(extractStatusFromPayload(responseLog.getPayload()));
        parsedData.setStatusCode(responseLog.getMetadata().getResponseStatus());
        
        logger.debug("📤 Updated ParsedAuditData with RESPONSE: {}", parsedData.getCorrelationId());
    }

    /**
     * Extract time from payload (payload/processed_date)
     */
    private LocalDateTime extractTimeFromPayload(Object payload) {
        try {
            if (payload instanceof java.util.Map) {
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> payloadMap = (java.util.Map<String, Object>) payload;
                Object processedDate = payloadMap.get("processed_date");
                if (processedDate != null) {
                    return LocalDateTime.parse(processedDate.toString(), DATE_TIME_FORMATTER);
                }
            }
        } catch (Exception e) {
            logger.debug("⚠️ Failed to extract time from payload: {}", e.getMessage());
        }
        return LocalDateTime.now();
    }

    /**
     * Extract status from payload (payload/status)
     */
    private String extractStatusFromPayload(Object payload) {
        try {
            if (payload instanceof java.util.Map) {
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> payloadMap = (java.util.Map<String, Object>) payload;
                Object status = payloadMap.get("status");
                if (status != null) {
                    return status.toString();
                }
            }
        } catch (Exception e) {
            logger.debug("⚠️ Failed to extract status from payload: {}", e.getMessage());
        }
        return "UNKNOWN";
    }

    /**
     * Send ParsedAuditData to Elasticsearch
     */
    @Async
    public void sendToElasticsearch(ParsedAuditData parsedData) {
        try {
            elasticsearchService.indexAuditData(parsedData);
            logger.info("📊 Successfully indexed audit data: {}", parsedData.getCorrelationId());
        } catch (Exception e) {
            logger.error("❌ Failed to index audit data {}: {}", 
                parsedData.getCorrelationId(), e.getMessage(), e);
        }
    }

    /**
     * Schedule timeout processing for a request ID
     */
    private void scheduleTimeoutProcessing(String requestId) {
        executorService.submit(() -> {
            try {
                Thread.sleep(timeoutMinutes * 60 * 1000); // Convert minutes to milliseconds
                processTimeout(requestId);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.debug("⏰ Timeout processing interrupted for: {}", requestId);
            }
        });
    }

    /**
     * Process timeout for a request ID
     * Index whatever data we have and clean up
     */
    private void processTimeout(String requestId) {
        ParsedAuditData pendingData = pendingRequests.get(requestId);
        
        if (pendingData != null) {
            // Timeout for pending request
            logger.info("⏰ Timeout reached for REQUEST: {}. Indexing with available data.", requestId);
            pendingData.setIsComplete(false);
            sendToElasticsearch(pendingData);
            pendingRequests.remove(requestId);
        } else if (requestId.endsWith("_orphaned")) {
            // Timeout for orphaned response
            String actualRequestId = requestId.replace("_orphaned", "");
            ParsedAuditData orphanedData = completedTransactions.get(actualRequestId);
            
            if (orphanedData != null) {
                logger.info("⏰ Timeout reached for ORPHANED RESPONSE: {}. Indexing with available data.", actualRequestId);
                sendToElasticsearch(orphanedData);
                completedTransactions.remove(actualRequestId);
            }
        }
    }

    /**
     * Scheduled cleanup task to remove old completed transactions
     */
    @Scheduled(fixedRateString = "${audit.processor.cleanup-interval-minutes:5}")
    public void cleanupOldTransactions() {
        try {
            int beforeSize = completedTransactions.size();
            completedTransactions.entrySet().removeIf(entry -> 
                entry.getValue().getIndexedAt().isBefore(LocalDateTime.now().minusMinutes(cleanupIntervalMinutes)));
            
            int removed = beforeSize - completedTransactions.size();
            if (removed > 0) {
                logger.info("🧹 Cleaned up {} old completed transactions", removed);
            }
        } catch (Exception e) {
            logger.error("❌ Error during cleanup: {}", e.getMessage(), e);
        }
    }

    /**
     * Get statistics about the processor
     */
    public String getStatistics() {
        return String.format("AuditDataProcessor Stats - Pending: %d, Completed: %d, Timeout: %d min", 
            pendingRequests.size(), completedTransactions.size(), timeoutMinutes);
    }
}
