package com.example.kafkaparsing.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public class KafkaConnectionDetails {

    @JsonProperty("kafkaBrokers")
    private List<String> kafkaBrokers;

    @JsonProperty("connectionName")
    private String connectionName;

    @JsonProperty("topic")
    private String topic;

    @JsonProperty("consumerGroupId")
    private String consumerGroupId;

    @JsonProperty("securityProtocol")
    private String securityProtocol;

    @JsonProperty("certificate")
    private String certificate;

    @JsonProperty("userName")
    private String userName;

    @JsonProperty("password")
    private String password;

    @JsonProperty("fields")
    private List<Map<String, Object>> fields;

    @JsonProperty("patterns")
    private Map<String, String> patterns;

    // Default constructor
    public KafkaConnectionDetails() {}

    // Getters and Setters
    public List<String> getKafkaBrokers() {
        return kafkaBrokers;
    }

    public void setKafkaBrokers(List<String> kafkaBrokers) {
        this.kafkaBrokers = kafkaBrokers;
    }

    public String getConnectionName() {
        return connectionName;
    }

    public void setConnectionName(String connectionName) {
        this.connectionName = connectionName;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getConsumerGroupId() {
        return consumerGroupId;
    }

    public void setConsumerGroupId(String consumerGroupId) {
        this.consumerGroupId = consumerGroupId;
    }

    public String getSecurityProtocol() {
        return securityProtocol;
    }

    public void setSecurityProtocol(String securityProtocol) {
        this.securityProtocol = securityProtocol;
    }

    public String getCertificate() {
        return certificate;
    }

    public void setCertificate(String certificate) {
        this.certificate = certificate;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public List<Map<String, Object>> getFields() {
        return fields;
    }

    public void setFields(List<Map<String, Object>> fields) {
        this.fields = fields;
    }

    public Map<String, String> getPatterns() {
        return patterns;
    }

    public void setPatterns(Map<String, String> patterns) {
        this.patterns = patterns;
    }

    @Override
    public String toString() {
        return "KafkaConnectionDetails{" +
                "kafkaBrokers=" + kafkaBrokers +
                ", connectionName='" + connectionName + '\'' +
                ", topic='" + topic + '\'' +
                ", consumerGroupId='" + consumerGroupId + '\'' +
                ", securityProtocol='" + securityProtocol + '\'' +
                ", certificate='" + certificate + '\'' +
                ", userName='" + userName + '\'' +
                ", password='[PROTECTED]'" +
                ", fields=" + fields +
                ", patterns=" + patterns +
                '}';
    }
}

