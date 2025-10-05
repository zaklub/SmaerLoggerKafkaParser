package com.example.kafkaparsing.entity;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "connections")
public class DataSourceConnection {

    @Id
    @Column(name = "uniqueid")
    private UUID uniqueId;

    @Column(name = "connectionname", nullable = false)
    private String connectionName;

    @Column(name = "connectiontype", nullable = false)
    private String connectionType;

    @Column(name = "details", nullable = false, length = 2000)
    private String details;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Default constructor
    public DataSourceConnection() {}

    // Constructor with parameters
    public DataSourceConnection(UUID uniqueId, String connectionName, String connectionType, 
                              String details, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.uniqueId = uniqueId;
        this.connectionName = connectionName;
        this.connectionType = connectionType;
        this.details = details;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and Setters
    public UUID getUniqueId() {
        return uniqueId;
    }

    public void setUniqueId(UUID uniqueId) {
        this.uniqueId = uniqueId;
    }

    public String getConnectionName() {
        return connectionName;
    }

    public void setConnectionName(String connectionName) {
        this.connectionName = connectionName;
    }

    public String getConnectionType() {
        return connectionType;
    }

    public void setConnectionType(String connectionType) {
        this.connectionType = connectionType;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "DataSourceConnection{" +
                "uniqueId=" + uniqueId +
                ", connectionName='" + connectionName + '\'' +
                ", connectionType='" + connectionType + '\'' +
                ", details='" + details + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}

