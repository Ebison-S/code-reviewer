package com.lens.apiservice.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEventPublisher implements EventPublisher {

    private final KafkaTemplate <String, String> kafkaTemplate;

    @Override
    public void publish (String topic, String payload) {
        kafkaTemplate.send (topic, payload).whenComplete ((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish event to topic: {}", topic, ex);
            } else {
                log.debug("Successfully published event to topic: {}", topic);
            }
        });
    }
}