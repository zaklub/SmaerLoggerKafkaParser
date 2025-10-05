package com.example.kafkaparsing.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
public class RawDataConsumer {

    private static final Logger logger = LoggerFactory.getLogger(RawDataConsumer.class);

    @KafkaListener(topics = "raw-data-topic_kafka", groupId = "raw-data-consumer-group")
    public void consumeRawData(@Payload String message,
                               @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                               @Header(KafkaHeaders.RECEIVED_PARTITION_ID) int partition,
                               @Header(KafkaHeaders.OFFSET) long offset,
                               @Header(value = KafkaHeaders.RECEIVED_MESSAGE_KEY, required = false) String key) {
        
        try {
            logger.info("════════════════════════════════════════════════════════════");
            logger.info("📨 RAW DATA MESSAGE RECEIVED");
            logger.info("════════════════════════════════════════════════════════════");
            logger.info("Topic      : {}", topic);
            logger.info("Partition  : {}", partition);
            logger.info("Offset     : {}", offset);
            logger.info("Key        : {}", key != null ? key : "null");
            logger.info("────────────────────────────────────────────────────────────");
            logger.info("📄 MESSAGE CONTENT:");
            logger.info("{}", message);
            logger.info("════════════════════════════════════════════════════════════");
            logger.info("");
            
        } catch (Exception e) {
            logger.error("❌ Error processing message from topic {}: {}", topic, e.getMessage(), e);
            logger.error("Raw message that failed: {}", message);
        }
    }
}



