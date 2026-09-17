package com.lens.apiservice.controller;

import com.lens.apiservice.service.GitHubWebhookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping ("/api/webhooks")
@RequiredArgsConstructor
public class GitHubWebhookController {

    private final GitHubWebhookService webhookService;

    @PostMapping ("/github")
    public ResponseEntity <Void> handleGitHubWebhook (
            @RequestHeader (value = "X-GitHub-Event", defaultValue = "unknown") String eventType,
            @RequestHeader (value = "X-Hub-Signature-256", defaultValue = "") String signatureHeader,
            @RequestBody String payload) {

        try {
            webhookService.processPullRequestEvent (eventType, signatureHeader, payload);
            return ResponseEntity.accepted ().build ();
        } catch (SecurityException e) {
            return ResponseEntity.status (HttpStatus.UNAUTHORIZED).build ();
        }
    }
}