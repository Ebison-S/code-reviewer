package com.lens.apiservice.messaging;

public interface EventPublisher {
    void publish (String topic, String payload);
}