package com.example.kafkaparsing.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.UUID;

public class ApiAuditLog {

    @JsonProperty("log_id")
    private String logId;

    @JsonProperty("api_name")
    private String apiName;

    @JsonProperty("request_id")
    private String requestId;

    @JsonProperty("timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    @JsonProperty("log_type")
    private String logType; // REQUEST or RESPONSE

    @JsonProperty("metadata")
    private ApiRequestMetadata metadata;

    @JsonProperty("payload")
    private Object payload;

    // Default constructor
    public ApiAuditLog() {
        this.logId = UUID.randomUUID().toString();
        this.timestamp = LocalDateTime.now();
    }

    // Constructor with parameters
    public ApiAuditLog(String apiName, String requestId, String logType, ApiRequestMetadata metadata, Object payload) {
        this();
        this.apiName = apiName;
        this.requestId = requestId;
        this.logType = logType;
        this.metadata = metadata;
        this.payload = payload;
    }

    // Getters and Setters
    public String getLogId() {
        return logId;
    }

    public void setLogId(String logId) {
        this.logId = logId;
    }

    public String getApiName() {
        return apiName;
    }

    public void setApiName(String apiName) {
        this.apiName = apiName;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getLogType() {
        return logType;
    }

    public void setLogType(String logType) {
        this.logType = logType;
    }

    public ApiRequestMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(ApiRequestMetadata metadata) {
        this.metadata = metadata;
    }

    public Object getPayload() {
        return payload;
    }

    public void setPayload(Object payload) {
        this.payload = payload;
    }

    @Override
    public String toString() {
        return "ApiAuditLog{" +
                "logId='" + logId + '\'' +
                ", apiName='" + apiName + '\'' +
                ", requestId='" + requestId + '\'' +
                ", timestamp=" + timestamp +
                ", logType='" + logType + '\'' +
                ", metadata=" + metadata +
                ", payload=" + payload +
                '}';
    }
}


