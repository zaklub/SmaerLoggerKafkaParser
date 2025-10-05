package com.example.kafkaparsing.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public class KafkaMessage {

    @JsonProperty("id")
    private String id;

    @JsonProperty("message")
    private String message;

    @JsonProperty("timestamp")
    private LocalDateTime timestamp;

    @JsonProperty("source")
    private String source;

    @JsonProperty("type")
    private String type;

    // Default constructor
    public KafkaMessage() {
        this.timestamp = LocalDateTime.now();
    }

    // Constructor with parameters
    public KafkaMessage(String id, String message, String source, String type) {
        this.id = id;
        this.message = message;
        this.source = source;
        this.type = type;
        this.timestamp = LocalDateTime.now();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "KafkaMessage{" +
                "id='" + id + '\'' +
                ", message='" + message + '\'' +
                ", timestamp=" + timestamp +
                ", source='" + source + '\'' +
                ", type='" + type + '\'' +
                '}';
    }
}

