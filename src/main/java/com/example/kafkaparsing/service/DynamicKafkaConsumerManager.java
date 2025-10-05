package com.example.kafkaparsing.service;

import com.example.kafkaparsing.entity.DataSourceConnection;
import com.example.kafkaparsing.model.KafkaConnectionDetails;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@DependsOn("kafkaMessageForwarder")
public class DynamicKafkaConsumerManager {

    private static final Logger logger = LoggerFactory.getLogger(DynamicKafkaConsumerManager.class);

    @Autowired
    private DataSourceConnectionService dataSourceConnectionService;

    @Autowired
    private KafkaMessageForwarder kafkaMessageForwarder;

    // Store active containers by connection ID
    private final Map<UUID, List<ConcurrentMessageListenerContainer<String, String>>> activeContainers = new ConcurrentHashMap<>();

    @PostConstruct
    public void initializeConsumers() {
        logger.info("Initializing dynamic Kafka consumers from database...");
        
        try {
            List<DataSourceConnection> connections = dataSourceConnectionService.loadKafkaConnections();
            
            if (connections.isEmpty()) {
                logger.warn("No Kafka connections found in database");
                return;
            }

            for (DataSourceConnection connection : connections) {
                try {
                    createConsumersForConnection(connection);
                } catch (Exception e) {
                    logger.error("Failed to create consumers for connection {}: {}", 
                        connection.getConnectionName(), e.getMessage(), e);
                }
            }

            logger.info("Dynamic Kafka consumer initialization complete. Active connections: {}", activeContainers.size());
            
        } catch (Exception e) {
            logger.error("Error during Kafka consumer initialization", e);
        }
    }

    /**
     * Create consumers for a single connection
     */
    private void createConsumersForConnection(DataSourceConnection connection) {
        logger.info("Creating consumers for connection: {} (ID: {})", 
            connection.getConnectionName(), connection.getUniqueId());

        // Parse connection details JSON
        KafkaConnectionDetails details = dataSourceConnectionService.parseConnectionDetails(connection.getDetails());

        // Validate
        if (!dataSourceConnectionService.validateConnectionDetails(details)) {
            logger.error("Invalid connection details for {}", connection.getConnectionName());
            return;
        }

        // Create consumer factory
        ConsumerFactory<String, String> consumerFactory = createConsumerFactory(details);

        // Create container factory
        ConcurrentKafkaListenerContainerFactory<String, String> containerFactory = 
            new ConcurrentKafkaListenerContainerFactory<>();
        containerFactory.setConsumerFactory(consumerFactory);

        // Create a container for the single topic
        List<ConcurrentMessageListenerContainer<String, String>> containers = new java.util.ArrayList<>();

        String topic = details.getTopic();
        if (topic != null && !topic.isEmpty()) {
            try {
                ConcurrentMessageListenerContainer<String, String> container = 
                    containerFactory.createContainer(topic);

                // Set message listener
                container.setupMessageListener((MessageListener<String, String>) record -> {
                    logger.info("Received message from topic '{}' (connection: {}): key={}, partition={}, offset={}", 
                        record.topic(), connection.getConnectionName(), record.key(), record.partition(), record.offset());

                    // Forward message to destination Kafka
                    try {
                        kafkaMessageForwarder.forwardMessage(record.key(), record.value(), null);
                    } catch (Exception e) {
                        logger.error("Error forwarding message from topic {}: {}", record.topic(), e.getMessage(), e);
                    }
                });

                // Start container
                container.start();
                containers.add(container);

                logger.info("Started consumer for topic '{}' on connection '{}'", topic, connection.getConnectionName());

            } catch (Exception e) {
                logger.error("Failed to create consumer for topic '{}' on connection '{}': {}", 
                    topic, connection.getConnectionName(), e.getMessage(), e);
            }
        }

        // Store containers
        activeContainers.put(connection.getUniqueId(), containers);

        logger.info("Created {} consumers for connection '{}'", containers.size(), connection.getConnectionName());
    }

    /**
     * Create Kafka consumer factory from connection details
     */
    private ConsumerFactory<String, String> createConsumerFactory(KafkaConnectionDetails details) {
        Map<String, Object> props = new HashMap<>();

        // Basic config - use first broker from the list
        String bootstrapServers = details.getKafkaBrokers().get(0);
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, details.getConsumerGroupId());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        // Default consumer config
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);

        // Security config
        if (details.getSecurityProtocol() != null) {
            props.put("security.protocol", details.getSecurityProtocol());
        }

        // Authentication if provided
        if (details.getUserName() != null && !details.getUserName().isEmpty() && 
            details.getPassword() != null && !details.getPassword().isEmpty()) {
            String jaasConfig = String.format(
                "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"%s\" password=\"%s\";",
                details.getUserName(), details.getPassword()
            );
            props.put("sasl.jaas.config", jaasConfig);
            props.put("sasl.mechanism", "PLAIN");
        }

        return new DefaultKafkaConsumerFactory<>(props);
    }


    @PreDestroy
    public void shutdown() {
        logger.info("Shutting down all Kafka consumers...");

        for (Map.Entry<UUID, List<ConcurrentMessageListenerContainer<String, String>>> entry : activeContainers.entrySet()) {
            for (ConcurrentMessageListenerContainer<String, String> container : entry.getValue()) {
                try {
                    container.stop();
                    logger.debug("Stopped consumer container for connection {}", entry.getKey());
                } catch (Exception e) {
                    logger.error("Error stopping consumer container: {}", e.getMessage(), e);
                }
            }
        }

        activeContainers.clear();
        logger.info("All Kafka consumers shut down");
    }
}

