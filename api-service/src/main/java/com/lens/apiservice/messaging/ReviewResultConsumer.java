package com.lens.apiservice.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lens.apiservice.client.GitHubClient;
import com.lens.apiservice.dto.ReviewResultPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor 
public class ReviewResultConsumer {

    private final ObjectMapper objectMapper;
    private final GitHubClient gitHubClient;

    @KafkaListener(topics = "review-results", groupId = "lens-gateway-group")
    public void consumeReviewResult(String message) {
        try {
            ReviewResultPayload payload = objectMapper.readValue(message, ReviewResultPayload.class);
            log.info("Consuming completed review for PR #{} in repo {}", 
                     payload.pullRequestNumber(), payload.repository());
            
            gitHubClient.postReviewComment(payload);
            log.info("Successfully published AI review back to GitHub PR #{}", payload.pullRequestNumber());
        } catch (Exception e) {
            log.error("Failed to process review result from Kafka or post to GitHub", e);
        }
    }
}