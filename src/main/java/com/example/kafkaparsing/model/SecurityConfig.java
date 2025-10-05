package com.example.kafkaparsing.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SecurityConfig {

    @JsonProperty("protocol")
    private String protocol; // PLAINTEXT, SASL_SSL, SSL, etc.

    @JsonProperty("sasl_mechanism")
    private String saslMechanism; // PLAIN, SCRAM-SHA-256, etc.

    @JsonProperty("username")
    private String username;

    @JsonProperty("password")
    private String password;

    @JsonProperty("ssl_truststore_location")
    private String sslTruststoreLocation;

    @JsonProperty("ssl_truststore_password")
    private String sslTruststorePassword;

    @JsonProperty("ssl_keystore_location")
    private String sslKeystoreLocation;

    @JsonProperty("ssl_keystore_password")
    private String sslKeystorePassword;

    // Default constructor
    public SecurityConfig() {}

    // Getters and Setters
    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getSaslMechanism() {
        return saslMechanism;
    }

    public void setSaslMechanism(String saslMechanism) {
        this.saslMechanism = saslMechanism;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getSslTruststoreLocation() {
        return sslTruststoreLocation;
    }

    public void setSslTruststoreLocation(String sslTruststoreLocation) {
        this.sslTruststoreLocation = sslTruststoreLocation;
    }

    public String getSslTruststorePassword() {
        return sslTruststorePassword;
    }

    public void setSslTruststorePassword(String sslTruststorePassword) {
        this.sslTruststorePassword = sslTruststorePassword;
    }

    public String getSslKeystoreLocation() {
        return sslKeystoreLocation;
    }

    public void setSslKeystoreLocation(String sslKeystoreLocation) {
        this.sslKeystoreLocation = sslKeystoreLocation;
    }

    public String getSslKeystorePassword() {
        return sslKeystorePassword;
    }

    public void setSslKeystorePassword(String sslKeystorePassword) {
        this.sslKeystorePassword = sslKeystorePassword;
    }

    @Override
    public String toString() {
        return "SecurityConfig{" +
                "protocol='" + protocol + '\'' +
                ", saslMechanism='" + saslMechanism + '\'' +
                ", username='" + username + '\'' +
                ", password='[PROTECTED]'" +
                ", sslTruststoreLocation='" + sslTruststoreLocation + '\'' +
                ", sslKeystoreLocation='" + sslKeystoreLocation + '\'' +
                '}';
    }
}



