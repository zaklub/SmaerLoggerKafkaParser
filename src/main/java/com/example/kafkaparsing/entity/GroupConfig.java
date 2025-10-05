package com.example.kafkaparsing.entity;

import javax.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "group_config")
public class GroupConfig {

    @Id
    @Column(name = "group_config_id")
    private BigDecimal groupConfigId;

    @Column(name = "group_name")
    private String groupName;

    @Column(name = "group_value")
    private String groupValue;

    @Column(name = "group_type")
    private String groupType;

    @Column(name = "description")
    private String description;

    // Default constructor
    public GroupConfig() {}

    // Constructor with parameters
    public GroupConfig(BigDecimal groupConfigId, String groupName, String groupValue, String groupType, String description) {
        this.groupConfigId = groupConfigId;
        this.groupName = groupName;
        this.groupValue = groupValue;
        this.groupType = groupType;
        this.description = description;
    }

    // Getters and Setters
    public BigDecimal getGroupConfigId() {
        return groupConfigId;
    }

    public void setGroupConfigId(BigDecimal groupConfigId) {
        this.groupConfigId = groupConfigId;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getGroupValue() {
        return groupValue;
    }

    public void setGroupValue(String groupValue) {
        this.groupValue = groupValue;
    }

    public String getGroupType() {
        return groupType;
    }

    public void setGroupType(String groupType) {
        this.groupType = groupType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "GroupConfig{" +
                "groupConfigId=" + groupConfigId +
                ", groupName='" + groupName + '\'' +
                ", groupValue='" + groupValue + '\'' +
                ", groupType='" + groupType + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
