package com.lens.apiservice.service;

import com.lens.apiservice.messaging.EventPublisher;
import com.lens.apiservice.security.WebhookSignatureValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class GitHubWebhookService {

    private final WebhookSignatureValidator signatureValidator;
    private final EventPublisher eventPublisher;

    public void processPullRequestEvent (String eventType, String signature, String payload) {
        if (!signatureValidator.isValid (payload, signature)) {
            log.warn ("Unauthorized webhook received. Signature validation failed.");
            throw new SecurityException ("Invalid webhook signature");
        }

        if ("pull_request".equals (eventType)) {
            log.info ("Processing pull_request event");
            eventPublisher.publish ("pr-events", payload);
        } else {
            log.debug ("Ignored event type: {}", eventType);
        }
    }
}