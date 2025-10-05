package com.example.kafkaparsing.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ApiMetadata {

    @JsonProperty("client_ip")
    private String clientIp;

    @JsonProperty("user_agent")
    private String userAgent;

    @JsonProperty("content_type")
    private String contentType;

    @JsonProperty("content_length")
    private Long contentLength;

    @JsonProperty("http_method")
    private String httpMethod;

    @JsonProperty("endpoint")
    private String endpoint;

    @JsonProperty("response_status")
    private Integer responseStatus;

    @JsonProperty("processing_time_ms")
    private Long processingTimeMs;

    // Default constructor
    public ApiMetadata() {}

    // Constructor with parameters
    public ApiMetadata(String clientIp, String userAgent, String contentType, Long contentLength, 
                      String httpMethod, String endpoint, Integer responseStatus, Long processingTimeMs) {
        this.clientIp = clientIp;
        this.userAgent = userAgent;
        this.contentType = contentType;
        this.contentLength = contentLength;
        this.httpMethod = httpMethod;
        this.endpoint = endpoint;
        this.responseStatus = responseStatus;
        this.processingTimeMs = processingTimeMs;
    }

    // Getters and Setters
    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Long getContentLength() {
        return contentLength;
    }

    public void setContentLength(Long contentLength) {
        this.contentLength = contentLength;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public Integer getResponseStatus() {
        return responseStatus;
    }

    public void setResponseStatus(Integer responseStatus) {
        this.responseStatus = responseStatus;
    }

    public Long getProcessingTimeMs() {
        return processingTimeMs;
    }

    public void setProcessingTimeMs(Long processingTimeMs) {
        this.processingTimeMs = processingTimeMs;
    }

    @Override
    public String toString() {
        return "ApiMetadata{" +
                "clientIp='" + clientIp + '\'' +
                ", userAgent='" + userAgent + '\'' +
                ", contentType='" + contentType + '\'' +
                ", contentLength=" + contentLength +
                ", httpMethod='" + httpMethod + '\'' +
                ", endpoint='" + endpoint + '\'' +
                ", responseStatus=" + responseStatus +
                ", processingTimeMs=" + processingTimeMs +
                '}';
    }
}


