package com.example.kafkaparsing.service;

import com.example.kafkaparsing.entity.ApiMetadataField;
import com.example.kafkaparsing.model.KafkaConnectionDetails;
import com.example.kafkaparsing.model.ParsedAuditData;
import com.fasterxml.jackson.databind.JsonNode;
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
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service for dynamic message processing that can handle any JSON structure
 * Uses database-driven field extraction based on api_metadata and api_metadata_field tables
 */
@Service
public class DynamicMessageProcessor {

    private static final Logger logger = LoggerFactory.getLogger(DynamicMessageProcessor.class);
    private static final DateTimeFormatter DEFAULT_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @Value("${audit.processor.timeout-minutes:1}")
    private int timeoutMinutes;

    @Value("${audit.processor.cleanup-interval-minutes:5}")
    private int cleanupIntervalMinutes;

    @Autowired
    private ElasticsearchService elasticsearchService;

    @Autowired
    private ApiMetadataService apiMetadataService;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    // Store pending requests by correlation ID
    private final ConcurrentHashMap<String, ParsedAuditData> pendingRequests = new ConcurrentHashMap<>();
    // Store completed transactions for cleanup
    private final ConcurrentHashMap<String, ParsedAuditData> completedTransactions = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newFixedThreadPool(5);

    @PostConstruct
    public void init() {
        logger.info("🔧 DynamicMessageProcessor initialized with timeout: {} minutes", timeoutMinutes);
    }

    @PreDestroy
    public void shutdown() {
        logger.info("🛑 Shutting down DynamicMessageProcessor");
        executorService.shutdown();
    }

    /**
     * Process incoming message dynamically based on database field configuration
     */
    public void processMessage(String message, String connectionName, KafkaConnectionDetails connectionDetails) {
        try {
            logger.debug("📨 Processing dynamic message from connection: {}", connectionName);
            logger.debug("Message length: {} characters", message.length());

            // Parse message as generic JSON
            JsonNode messageNode = objectMapper.readTree(message);
            
            // Extract API name from message (extractedApiName added by DynamicKafkaConsumerManager)
            String extractedApiName = extractFieldByPath(messageNode, "extractedApiName");
            
            if (extractedApiName == null) {
                logger.warn("⚠️ No extractedApiName found in message from connection: {}", connectionName);
                return;
            }
            
            logger.info("🔍 Extracted API Name: {} from connection: {}", extractedApiName, connectionName);
            
            // Load field configuration from database for this API
            List<ApiMetadataField> fieldConfig = apiMetadataService.getFieldConfigurationForApi(extractedApiName);
            
            if (fieldConfig.isEmpty()) {
                logger.warn("⚠️ No field configuration found for API: {}", extractedApiName);
                return;
            }
            
            // Extract log type to determine if it's REQUEST or RESPONSE
            String logType = extractFieldByPath(messageNode, "log_type");
            String correlationId = extractFieldByPath(messageNode, "request_id");
            
            if (correlationId == null) {
                logger.warn("⚠️ No correlation ID found in message from connection: {}", connectionName);
                return;
            }

            if ("REQUEST".equals(logType)) {
                processRequest(messageNode, connectionName, extractedApiName, fieldConfig, correlationId);
            } else if ("RESPONSE".equals(logType)) {
                processResponse(messageNode, connectionName, extractedApiName, fieldConfig, correlationId);
            } else {
                logger.warn("⚠️ Unknown log type '{}' in message from connection: {}", logType, connectionName);
                // Process as single message without correlation
                processSingleMessage(messageNode, connectionName, extractedApiName, fieldConfig, correlationId);
            }

        } catch (Exception e) {
            logger.error("❌ Error processing dynamic message from connection {}: {}", connectionName, e.getMessage(), e);
        }
    }

    /**
     * Process REQUEST message
     */
    private void processRequest(JsonNode messageNode, String connectionName, String apiName, List<ApiMetadataField> fieldConfig, String correlationId) {
        logger.info("📝 Processing REQUEST - ID: {}, API: {}, Connection: {}", correlationId, apiName, connectionName);

        ParsedAuditData parsedData = createParsedDataFromMessage(messageNode, connectionName, apiName, fieldConfig, correlationId, "REQUEST");
        pendingRequests.put(correlationId, parsedData);

        // Schedule timeout processing
        scheduleTimeoutProcessing(correlationId);

        logger.info("⏳ REQUEST stored, waiting for RESPONSE. Pending requests: {}", pendingRequests.size());
    }

    /**
     * Process RESPONSE message
     */
    private void processResponse(JsonNode messageNode, String connectionName, String apiName, List<ApiMetadataField> fieldConfig, String correlationId) {
        logger.info("📤 Processing RESPONSE - ID: {}, API: {}, Connection: {}", correlationId, apiName, connectionName);

        ParsedAuditData pendingData = pendingRequests.get(correlationId);
        
        if (pendingData != null) {
            // Found matching request, complete the transaction
            completeTransaction(pendingData, messageNode, fieldConfig);
            pendingRequests.remove(correlationId);
            
            logger.info("✅ Transaction completed for ID: {}. Remaining pending: {}", correlationId, pendingRequests.size());
        } else {
            // Orphaned response - wait for request
            logger.info("🔄 Orphaned RESPONSE detected for ID: {}. Waiting for REQUEST...", correlationId);
            handleOrphanedResponse(messageNode, connectionName, apiName, fieldConfig, correlationId);
        }
    }

    /**
     * Process single message (no REQUEST/RESPONSE correlation)
     */
    private void processSingleMessage(JsonNode messageNode, String connectionName, String apiName, List<ApiMetadataField> fieldConfig, String correlationId) {
        logger.info("📄 Processing SINGLE message - ID: {}, API: {}, Connection: {}", correlationId, apiName, connectionName);

        ParsedAuditData parsedData = createParsedDataFromMessage(messageNode, connectionName, apiName, fieldConfig, correlationId, "SINGLE");
        parsedData.setIsComplete(true);
        
        // Send to Elasticsearch immediately
        sendToElasticsearch(parsedData);
        
        logger.info("🎯 Single message processed and indexed: {}", correlationId);
    }

    /**
     * Handle orphaned response (response without matching request)
     */
    private void handleOrphanedResponse(JsonNode messageNode, String connectionName, String apiName, List<ApiMetadataField> fieldConfig, String correlationId) {
        ParsedAuditData orphanedData = createParsedDataFromMessage(messageNode, connectionName, apiName, fieldConfig, correlationId, "RESPONSE");
        orphanedData.setIsComplete(false);
        
        completedTransactions.put(correlationId, orphanedData);
        
        // Schedule timeout for orphaned response
        scheduleTimeoutProcessing(correlationId + "_orphaned");
        
        logger.info("🔄 Orphaned RESPONSE stored for ID: {}. Waiting for REQUEST...", correlationId);
    }

    /**
     * Complete transaction by merging request and response data
     */
    private void completeTransaction(ParsedAuditData parsedData, JsonNode responseNode, List<ApiMetadataField> fieldConfig) {
        updateParsedDataFromResponse(parsedData, responseNode, fieldConfig);
        parsedData.setIsComplete(true);
        
        // Send to Elasticsearch
        sendToElasticsearch(parsedData);
        
        logger.info("🎯 Transaction completed and indexed: {}", parsedData.getCorrelationId());
    }

    /**
     * Create ParsedAuditData from message using database-driven field extraction
     * messageType: "REQUEST", "RESPONSE", or "SINGLE"
     */
    private ParsedAuditData createParsedDataFromMessage(JsonNode messageNode, String connectionName, String apiName, 
                                                        List<ApiMetadataField> fieldConfig, String correlationId, String messageType) {
        ParsedAuditData parsedData = new ParsedAuditData(correlationId);
        
        // Set API name
        parsedData.setApiName(apiName);
        
        // Extract fields using database configuration (only mandatory fields)
        // Pass messageType to know whether to extract RequestTime or ResponseTime
        extractFieldsFromMessage(messageNode, parsedData, fieldConfig, messageType);
        
        // Store original message as request payload
        try {
            parsedData.setRequestPayload(objectMapper.writeValueAsString(messageNode));
        } catch (Exception e) {
            logger.warn("⚠️ Failed to serialize request payload: {}", e.getMessage());
            parsedData.setRequestPayload("{}");
        }
        
        logger.debug("📝 Created ParsedAuditData from message: {} (API: {}, Type: {})", correlationId, apiName, messageType);
        return parsedData;
    }

    /**
     * Update ParsedAuditData with response message data
     */
    private void updateParsedDataFromResponse(ParsedAuditData parsedData, JsonNode responseNode, List<ApiMetadataField> fieldConfig) {
        // Extract response fields (only response-specific fields)
        extractFieldsFromMessage(responseNode, parsedData, fieldConfig, "RESPONSE");
        
        // Store response payload
        try {
            parsedData.setResponsePayload(objectMapper.writeValueAsString(responseNode));
        } catch (Exception e) {
            logger.warn("⚠️ Failed to serialize response payload: {}", e.getMessage());
            parsedData.setResponsePayload("{}");
        }
        
        logger.debug("📤 Updated ParsedAuditData with response: {}", parsedData.getCorrelationId());
    }

    /**
     * Extract fields from message using database field configuration (Mandatory + Custom)
     * Context-aware: Only extracts RequestTime from REQUEST, ResponseTime from RESPONSE
     * Custom fields only extracted from REQUEST messages
     */
    private void extractFieldsFromMessage(JsonNode messageNode, ParsedAuditData parsedData, 
                                         List<ApiMetadataField> fieldConfig, String messageType) {
        
        int mandatoryCount = 0;
        int customCount = 0;
        
        logger.debug("📋 Extracting fields from {} message", messageType);
        
        for (ApiMetadataField field : fieldConfig) {
            try {
                String keyStatus = field.getKeyStatus();
                String elasticsearchField = field.getField();
                String jsonPath = field.getPath();
                String datatype = field.getDatatype();
                String datePattern = field.getDatePatternString();
                
                if ("Mandatory".equals(keyStatus)) {
                    // Process Mandatory fields
                    extractMandatoryField(messageNode, parsedData, field, messageType, elasticsearchField, jsonPath, datatype, datePattern);
                    mandatoryCount++;
                } 
                else if ("Custom".equals(keyStatus)) {
                    // Process Custom fields - check message_type from database
                    String fieldMessageType = field.getMessageType();
                    
                    // If message_type is specified, check if it matches
                    if (fieldMessageType != null && !fieldMessageType.isEmpty()) {
                        if (fieldMessageType.equalsIgnoreCase(messageType)) {
                            extractCustomField(messageNode, parsedData, elasticsearchField, jsonPath, fieldMessageType);
                            customCount++;
                        } else {
                            logger.debug("   ⏭️ Skipping Custom field '{}' (requires {} message, current: {})", 
                                elasticsearchField, fieldMessageType, messageType);
                        }
                    } else {
                        // If message_type is NULL, extract from current message
                        extractCustomField(messageNode, parsedData, elasticsearchField, jsonPath, "ANY");
                        customCount++;
                    }
                }
                
            } catch (Exception e) {
                logger.warn("⚠️ Failed to extract field {}: {}", field.getIdentifier(), e.getMessage());
            }
        }
        
        logger.debug("📊 Extracted {} Mandatory fields and {} Custom fields from {} message", 
            mandatoryCount, customCount, messageType);
    }

    /**
     * Extract mandatory field with database-driven context awareness
     */
    private void extractMandatoryField(JsonNode messageNode, ParsedAuditData parsedData, ApiMetadataField field,
                                      String messageType, String elasticsearchField, String jsonPath, 
                                      String datatype, String datePattern) {
        
        // Check if this field should be extracted from current message type
        String fieldMessageType = field.getMessageType();
        
        if (fieldMessageType != null && !fieldMessageType.isEmpty()) {
            // Field has specific message type requirement
            if (!fieldMessageType.equalsIgnoreCase(messageType)) {
                logger.debug("   ⏭️ Skipping {} (requires {} message, current: {})", 
                    elasticsearchField, fieldMessageType, messageType);
                return;
            }
        }
        // If message_type is NULL, extract from both REQUEST and RESPONSE
        
        if (jsonPath != null && elasticsearchField != null) {
            String value = extractFieldByPath(messageNode, jsonPath);
            if (value != null) {
                setFieldValue(parsedData, elasticsearchField, value, datatype, datePattern);
                logger.debug("   ✅ Mandatory Field - {}: {} = {} (MsgType: {})", elasticsearchField, jsonPath, 
                    value.length() > 50 ? value.substring(0, 50) + "..." : value, 
                    fieldMessageType != null ? fieldMessageType : "ANY");
            } else {
                logger.debug("   ⚠️ Mandatory Field {} not found at path: {}", elasticsearchField, jsonPath);
            }
        }
    }

    /**
     * Extract custom field and add to CustomField array
     */
    private void extractCustomField(JsonNode messageNode, ParsedAuditData parsedData, 
                                   String fieldName, String jsonPath, String fieldMessageType) {
        if (jsonPath != null && fieldName != null) {
            String value = extractFieldByPath(messageNode, jsonPath);
            if (value != null) {
                // Add to CustomField array with field name as key
                parsedData.addCustomField(fieldName, value);
                logger.debug("   ✅ Custom Field - {}: {} = {} (MsgType: {})", fieldName, jsonPath, 
                    value.length() > 50 ? value.substring(0, 50) + "..." : value, fieldMessageType);
            } else {
                logger.debug("   ⚠️ Custom Field {} not found at path: {}", fieldName, jsonPath);
            }
        }
    }

    /**
     * Extract field value using JSON path (supports dot notation)
     */
    private String extractFieldByPath(JsonNode node, String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }
        
        try {
            String[] pathParts = path.split("\\.");
            JsonNode current = node;
            
            for (String part : pathParts) {
                if (current == null) return null;
                current = current.get(part);
            }
            
            return current != null && !current.isNull() ? current.asText() : null;
        } catch (Exception e) {
            logger.debug("⚠️ Failed to extract field by path '{}': {}", path, e.getMessage());
            return null;
        }
    }

    /**
     * Set field value in ParsedAuditData based on field name and datatype
     */
    private void setFieldValue(ParsedAuditData parsedData, String fieldName, String value, String datatype, String datePattern) {
        if (value == null || fieldName == null) return;
        
        try {
            switch (fieldName) {
                case "APIName":
                    parsedData.setApiName(value);
                    break;
                case "CorrelationID":
                    parsedData.setCorrelationId(value);
                    break;
                case "Host":
                    parsedData.setHost(value);
                    break;
                case "ParentID":
                    parsedData.setParentId(value);
                    break;
                case "RequestPayload":
                    parsedData.setRequestPayload(value);
                    break;
                case "ResourcePath":
                    parsedData.setResourcePath(value);
                    break;
                case "ResponsePayload":
                    parsedData.setResponsePayload(value);
                    break;
                case "Status":
                    parsedData.setStatus(value);
                    break;
                case "StatusCode":
                    try {
                        parsedData.setStatusCode(Integer.parseInt(value));
                    } catch (NumberFormatException e) {
                        logger.debug("⚠️ Invalid status code: {}", value);
                    }
                    break;
                case "TransactionID":
                    parsedData.setTransactionId(value);
                    break;
                case "UniqueTransactionID":
                    parsedData.setUniqueTransactionId(value);
                    break;
                case "RequestTime":
                    LocalDateTime requestTime = parseDateTime(value, datePattern);
                    if (requestTime != null) {
                        parsedData.setRequestTime(requestTime);
                        logger.debug("   ✅ Set RequestTime: {}", requestTime);
                    }
                    break;
                case "ResponseTime":
                    LocalDateTime responseTime = parseDateTime(value, datePattern);
                    if (responseTime != null) {
                        parsedData.setResponseTime(responseTime);
                        logger.debug("   ✅ Set ResponseTime: {}", responseTime);
                    }
                    break;
                default:
                    logger.debug("⚠️ Unknown Elasticsearch field name: {}", fieldName);
            }
        } catch (Exception e) {
            logger.debug("⚠️ Failed to set field {}: {}", fieldName, e.getMessage());
        }
    }

    /**
     * Parse date time string with optional custom pattern
     */
    private LocalDateTime parseDateTime(String dateTimeStr, String pattern) {
        if (dateTimeStr == null) return null;
        
        try {
            // Use custom pattern if provided
            if (pattern != null && !pattern.isEmpty()) {
                DateTimeFormatter customFormatter = DateTimeFormatter.ofPattern(pattern);
                return LocalDateTime.parse(dateTimeStr, customFormatter);
            }
            
            // Use default pattern
            return LocalDateTime.parse(dateTimeStr, DEFAULT_DATE_TIME_FORMATTER);
        } catch (Exception e) {
            logger.debug("⚠️ Failed to parse date time '{}' with pattern '{}': {}", 
                dateTimeStr, pattern != null ? pattern : "default", e.getMessage());
            return null;
        }
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
     * Schedule timeout processing for a correlation ID
     */
    private void scheduleTimeoutProcessing(String correlationId) {
        executorService.submit(() -> {
            try {
                Thread.sleep(timeoutMinutes * 60 * 1000); // Convert minutes to milliseconds
                processTimeout(correlationId);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.debug("⏰ Timeout processing interrupted for: {}", correlationId);
            }
        });
    }

    /**
     * Process timeout for a correlation ID
     */
    private void processTimeout(String correlationId) {
        ParsedAuditData pendingData = pendingRequests.get(correlationId);
        
        if (pendingData != null) {
            // Timeout for pending request
            logger.info("⏰ Timeout reached for REQUEST: {}. Indexing with available data.", correlationId);
            pendingData.setIsComplete(false);
            sendToElasticsearch(pendingData);
            pendingRequests.remove(correlationId);
        } else if (correlationId.endsWith("_orphaned")) {
            // Timeout for orphaned response
            String actualCorrelationId = correlationId.replace("_orphaned", "");
            ParsedAuditData orphanedData = completedTransactions.get(actualCorrelationId);
            
            if (orphanedData != null) {
                logger.info("⏰ Timeout reached for ORPHANED RESPONSE: {}. Indexing with available data.", actualCorrelationId);
                sendToElasticsearch(orphanedData);
                completedTransactions.remove(actualCorrelationId);
            }
        }
    }

    /**
     * Scheduled cleanup task
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
        return String.format("DynamicMessageProcessor Stats - Pending: %d, Completed: %d, Timeout: %d min", 
            pendingRequests.size(), completedTransactions.size(), timeoutMinutes);
    }
}

