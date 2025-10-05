package com.example.kafkaparsing.service;

import com.example.kafkaparsing.model.KafkaMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

import java.util.UUID;

@Service
public class KafkaProducerService {

    private static final Logger logger = LoggerFactory.getLogger(KafkaProducerService.class);
    private static final String TOPIC_NAME = "api_audit_zak_logs";

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Send a message to Kafka topic
     */
    public void sendMessage(KafkaMessage message) {
        try {
            // Generate a unique key for the message
            String key = UUID.randomUUID().toString();
            
            logger.info("Sending message to Kafka topic: {}", TOPIC_NAME);
            logger.info("Message: {}", message);
            
            // Send message to Kafka
            ListenableFuture<SendResult<String, Object>> future = kafkaTemplate.send(TOPIC_NAME, key, message);
            
            // Add callback to handle success/failure
            future.addCallback(new ListenableFutureCallback<SendResult<String, Object>>() {
                @Override
                public void onSuccess(SendResult<String, Object> result) {
                    logger.info("Message sent successfully to topic: {} with offset: {}", 
                              TOPIC_NAME, result.getRecordMetadata().offset());
                }

                @Override
                public void onFailure(Throwable ex) {
                    logger.error("Failed to send message to topic: {}", TOPIC_NAME, ex);
                }
            });
            
        } catch (Exception e) {
            logger.error("Error sending message to Kafka", e);
            throw new RuntimeException("Failed to send message to Kafka", e);
        }
    }

    /**
     * Send a simple string message to Kafka topic
     */
    public void sendSimpleMessage(String message) {
        KafkaMessage kafkaMessage = new KafkaMessage();
        kafkaMessage.setId(UUID.randomUUID().toString());
        kafkaMessage.setMessage(message);
        kafkaMessage.setSource("kafka-parsing-app");
        kafkaMessage.setType("simple-message");
        
        sendMessage(kafkaMessage);
    }

    /**
     * Send a custom message with all fields
     */
    public void sendCustomMessage(String id, String message, String source, String type) {
        KafkaMessage kafkaMessage = new KafkaMessage(id, message, source, type);
        sendMessage(kafkaMessage);
    }
}
