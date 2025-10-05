package com.example.kafkaparsing.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RetryConfig {

    @JsonProperty("max_attempts")
    private int maxAttempts = 3; // Default: 3 retries

    @JsonProperty("backoff_ms")
    private long backoffMs = 1000; // Default: 1 second

    // Default constructor
    public RetryConfig() {}

    public RetryConfig(int maxAttempts, long backoffMs) {
        this.maxAttempts = maxAttempts;
        this.backoffMs = backoffMs;
    }

    // Getters and Setters
    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public long getBackoffMs() {
        return backoffMs;
    }

    public void setBackoffMs(long backoffMs) {
        this.backoffMs = backoffMs;
    }

    @Override
    public String toString() {
        return "RetryConfig{" +
                "maxAttempts=" + maxAttempts +
                ", backoffMs=" + backoffMs +
                '}';
    }
}



