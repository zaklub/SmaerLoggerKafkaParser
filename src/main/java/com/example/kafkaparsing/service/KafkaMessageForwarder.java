package com.example.kafkaparsing.service;

import com.example.kafkaparsing.model.RetryConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaMessageForwarder {

    private static final Logger logger = LoggerFactory.getLogger(KafkaMessageForwarder.class);
    private static final String DESTINATION_TOPIC = "raw-data-topic_kafka";

    @Autowired
    private KafkaTemplate<String, String> stringKafkaTemplate;

    /**
     * Forward message to destination Kafka with retry logic
     */
    public void forwardMessage(String key, Object value, RetryConfig retryConfig) {
        if (retryConfig == null) {
            retryConfig = new RetryConfig(3, 1000); // Default: 3 attempts, 1 second backoff
        }

        int attempts = 0;
        Exception lastException = null;
        
        // Convert value to String if it's not already
        String messageValue = (value instanceof String) ? (String) value : String.valueOf(value);

        while (attempts < retryConfig.getMaxAttempts()) {
            try {
                logger.debug("Forwarding message to {} (attempt {}/{})", DESTINATION_TOPIC, attempts + 1, retryConfig.getMaxAttempts());
                
                stringKafkaTemplate.send(DESTINATION_TOPIC, key, messageValue).get(); // Blocking send with .get()
                
                logger.info("Successfully forwarded message to {} on attempt {}", DESTINATION_TOPIC, attempts + 1);
                return; // Success - exit method
                
            } catch (Exception e) {
                attempts++;
                lastException = e;
                
                logger.warn("Failed to forward message to {} (attempt {}/{}): {}", 
                    DESTINATION_TOPIC, attempts, retryConfig.getMaxAttempts(), e.getMessage());
                
                if (attempts < retryConfig.getMaxAttempts()) {
                    try {
                        logger.debug("Waiting {} ms before retry", retryConfig.getBackoffMs());
                        Thread.sleep(retryConfig.getBackoffMs());
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        logger.error("Retry interrupted", ie);
                        break;
                    }
                }
            }
        }

        // All retries failed
        logger.error("Failed to forward message to {} after {} attempts. Last error: {}", 
            DESTINATION_TOPIC, retryConfig.getMaxAttempts(), 
            lastException != null ? lastException.getMessage() : "Unknown error");
    }

    /**
     * Forward message with default retry config
     */
    public void forwardMessage(String key, Object value) {
        forwardMessage(key, value, null);
    }
}

