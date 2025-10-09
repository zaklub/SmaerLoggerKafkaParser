package com.example.kafkaparsing.entity;

import javax.persistence.*;
import java.util.UUID;

/**
 * Entity for api_metadata_field table
 */
@Entity
@Table(name = "api_metadata_field")
public class ApiMetadataField {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "datatype")
    private String datatype;

    @Column(name = "field")
    private String field;

    @Column(name = "identifier")
    private String identifier;

    @Column(name = "key_status")
    private String keyStatus;

    @Column(name = "path")
    private String path;

    @Column(name = "api_metadata_id")
    private UUID apiMetadataId;

    @Column(name = "date_pattern_string")
    private String datePatternString;

    // Constructors
    public ApiMetadataField() {
    }

    public ApiMetadataField(Long id) {
        this.id = id;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getDatatype() {
        return datatype;
    }

    public void setDatatype(String datatype) {
        this.datatype = datatype;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getKeyStatus() {
        return keyStatus;
    }

    public void setKeyStatus(String keyStatus) {
        this.keyStatus = keyStatus;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public UUID getApiMetadataId() {
        return apiMetadataId;
    }

    public void setApiMetadataId(UUID apiMetadataId) {
        this.apiMetadataId = apiMetadataId;
    }

    public String getDatePatternString() {
        return datePatternString;
    }

    public void setDatePatternString(String datePatternString) {
        this.datePatternString = datePatternString;
    }

    @Override
    public String toString() {
        return "ApiMetadataField{" +
                "id=" + id +
                ", contentType='" + contentType + '\'' +
                ", datatype='" + datatype + '\'' +
                ", field='" + field + '\'' +
                ", identifier='" + identifier + '\'' +
                ", keyStatus='" + keyStatus + '\'' +
                ", path='" + path + '\'' +
                ", apiMetadataId=" + apiMetadataId +
                ", datePatternString='" + datePatternString + '\'' +
                '}';
    }
}

