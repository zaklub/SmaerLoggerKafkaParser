package com.example.kafkaparsing.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;

/**
 * Model representing the parsed audit data that will be indexed in Elasticsearch
 * This represents the final structure after correlating Request and Response messages
 */
@Document(indexName = "my_smartlogger_index")
public class ParsedAuditData {

    @Id
    private String id;

    @JsonProperty("APIName")
    @Field(type = FieldType.Keyword)
    private String apiName;

    @JsonProperty("CorrelationID")
    @Field(type = FieldType.Keyword)
    private String correlationId;

    @JsonProperty("Host")
    @Field(type = FieldType.Keyword)
    private String host;

    @JsonProperty("ParentID")
    @Field(type = FieldType.Keyword)
    private String parentId;

    @JsonProperty("RequestPayload")
    @Field(type = FieldType.Text)
    private String requestPayload;

    @JsonProperty("RequestTime")
    @Field(type = FieldType.Date)
    private LocalDateTime requestTime;

    @JsonProperty("ResourcePath")
    @Field(type = FieldType.Keyword)
    private String resourcePath;

    @JsonProperty("ResponsePayload")
    @Field(type = FieldType.Text)
    private String responsePayload;

    @JsonProperty("ResponseTime")
    @Field(type = FieldType.Date)
    private LocalDateTime responseTime;

    @JsonProperty("Status")
    @Field(type = FieldType.Keyword)
    private String status;

    @JsonProperty("StatusCode")
    @Field(type = FieldType.Integer)
    private Integer statusCode;

    @JsonProperty("TransactionID")
    @Field(type = FieldType.Keyword)
    private String transactionId;

    @JsonProperty("UniqueTransactionID")
    @Field(type = FieldType.Keyword)
    private String uniqueTransactionId;

    // Additional fields for tracking
    @Field(type = FieldType.Date)
    private LocalDateTime indexedAt;

    @Field(type = FieldType.Boolean)
    private Boolean isComplete; // true if both request and response were found

    // Constructors
    public ParsedAuditData() {
        this.indexedAt = LocalDateTime.now();
        this.isComplete = false;
    }

    public ParsedAuditData(String correlationId) {
        this();
        this.correlationId = correlationId;
        this.parentId = correlationId;
        this.transactionId = correlationId;
        this.uniqueTransactionId = correlationId;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getApiName() {
        return apiName;
    }

    public void setApiName(String apiName) {
        this.apiName = apiName;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public String getRequestPayload() {
        return requestPayload;
    }

    public void setRequestPayload(String requestPayload) {
        this.requestPayload = requestPayload;
    }

    public LocalDateTime getRequestTime() {
        return requestTime;
    }

    public void setRequestTime(LocalDateTime requestTime) {
        this.requestTime = requestTime;
    }

    public String getResourcePath() {
        return resourcePath;
    }

    public void setResourcePath(String resourcePath) {
        this.resourcePath = resourcePath;
    }

    public String getResponsePayload() {
        return responsePayload;
    }

    public void setResponsePayload(String responsePayload) {
        this.responsePayload = responsePayload;
    }

    public LocalDateTime getResponseTime() {
        return responseTime;
    }

    public void setResponseTime(LocalDateTime responseTime) {
        this.responseTime = responseTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getUniqueTransactionId() {
        return uniqueTransactionId;
    }

    public void setUniqueTransactionId(String uniqueTransactionId) {
        this.uniqueTransactionId = uniqueTransactionId;
    }

    public LocalDateTime getIndexedAt() {
        return indexedAt;
    }

    public void setIndexedAt(LocalDateTime indexedAt) {
        this.indexedAt = indexedAt;
    }

    public Boolean getIsComplete() {
        return isComplete;
    }

    public void setIsComplete(Boolean isComplete) {
        this.isComplete = isComplete;
    }

    @Override
    public String toString() {
        return "ParsedAuditData{" +
                "id='" + id + '\'' +
                ", apiName='" + apiName + '\'' +
                ", correlationId='" + correlationId + '\'' +
                ", host='" + host + '\'' +
                ", parentId='" + parentId + '\'' +
                ", resourcePath='" + resourcePath + '\'' +
                ", status='" + status + '\'' +
                ", statusCode=" + statusCode +
                ", transactionId='" + transactionId + '\'' +
                ", isComplete=" + isComplete +
                ", indexedAt=" + indexedAt +
                '}';
    }
}
