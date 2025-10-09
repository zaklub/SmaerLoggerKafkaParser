package com.example.kafkaparsing.entity;

import javax.persistence.*;
import java.util.UUID;

/**
 * Entity for api_metadata table
 */
@Entity
@Table(name = "api_metadata")
public class ApiMetadata {

    @Id
    @Column(name = "unique_id")
    private UUID uniqueId;

    @Column(name = "api_name")
    private String apiName;

    @Column(name = "connection_name")
    private String connectionName;

    @Column(name = "dataset")
    private String dataset;

    @Column(name = "resource_path")
    private String resourcePath;

    @Column(name = "role_names")
    private String roleNames;

    @Column(name = "status")
    private String status;

    @Column(name = "api_content_type")
    private String apiContentType;

    // Constructors
    public ApiMetadata() {
    }

    public ApiMetadata(UUID uniqueId) {
        this.uniqueId = uniqueId;
    }

    // Getters and Setters
    public UUID getUniqueId() {
        return uniqueId;
    }

    public void setUniqueId(UUID uniqueId) {
        this.uniqueId = uniqueId;
    }

    public String getApiName() {
        return apiName;
    }

    public void setApiName(String apiName) {
        this.apiName = apiName;
    }

    public String getConnectionName() {
        return connectionName;
    }

    public void setConnectionName(String connectionName) {
        this.connectionName = connectionName;
    }

    public String getDataset() {
        return dataset;
    }

    public void setDataset(String dataset) {
        this.dataset = dataset;
    }

    public String getResourcePath() {
        return resourcePath;
    }

    public void setResourcePath(String resourcePath) {
        this.resourcePath = resourcePath;
    }

    public String getRoleNames() {
        return roleNames;
    }

    public void setRoleNames(String roleNames) {
        this.roleNames = roleNames;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getApiContentType() {
        return apiContentType;
    }

    public void setApiContentType(String apiContentType) {
        this.apiContentType = apiContentType;
    }

    @Override
    public String toString() {
        return "ApiMetadata{" +
                "uniqueId=" + uniqueId +
                ", apiName='" + apiName + '\'' +
                ", connectionName='" + connectionName + '\'' +
                ", dataset='" + dataset + '\'' +
                ", resourcePath='" + resourcePath + '\'' +
                ", status='" + status + '\'' +
                ", apiContentType='" + apiContentType + '\'' +
                '}';
    }
}

