package com.example.kafkaparsing.service;

import com.example.kafkaparsing.entity.DataSourceConnection;
import com.example.kafkaparsing.model.KafkaConnectionDetails;
import com.example.kafkaparsing.repository.DataSourceConnectionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class DataSourceConnectionService {

    private static final Logger logger = LoggerFactory.getLogger(DataSourceConnectionService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private DataSourceConnectionRepository dataSourceConnectionRepository;

    /**
     * Load all Kafka connections from database
     */
    public List<DataSourceConnection> loadKafkaConnections() {
        try {
            List<DataSourceConnection> connections = dataSourceConnectionRepository.findByConnectionType("kafka");
            logger.info("Loaded {} Kafka connections from database", connections.size());
            return connections;
        } catch (Exception e) {
            logger.error("Error loading Kafka connections from database", e);
            return new ArrayList<>();
        }
    }

    /**
     * Parse JSON details column to KafkaConnectionDetails object
     */
    public KafkaConnectionDetails parseConnectionDetails(String detailsJson) {
        try {
            KafkaConnectionDetails details = objectMapper.readValue(detailsJson, KafkaConnectionDetails.class);
            logger.debug("Parsed connection details: {}", details);
            return details;
        } catch (Exception e) {
            logger.error("Error parsing connection details JSON: {}", detailsJson, e);
            throw new RuntimeException("Failed to parse Kafka connection details", e);
        }
    }

    /**
     * Validate connection details
     */
    public boolean validateConnectionDetails(KafkaConnectionDetails details) {
        if (details == null) {
            logger.warn("Connection details is null");
            return false;
        }

        if (details.getKafkaBrokers() == null || details.getKafkaBrokers().isEmpty()) {
            logger.warn("Kafka brokers list is null or empty");
            return false;
        }

        if (details.getTopic() == null || details.getTopic().isEmpty()) {
            logger.warn("Topic is null or empty");
            return false;
        }

        if (details.getConsumerGroupId() == null || details.getConsumerGroupId().isEmpty()) {
            logger.warn("Consumer group ID is null or empty");
            return false;
        }

        return true;
    }
}

