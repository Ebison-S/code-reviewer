package com.lens.apiservice.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lens.apiservice.service.GitHubAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewResultConsumer {

    private final ObjectMapper objectMapper;
    private final GitHubAuthService gitHubAuthService;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @KafkaListener(topics = "review-results", groupId = "lens-gateway-group")
    public void consumeReviewResult(String message) {
        try {
            JsonNode root = objectMapper.readTree(message);
            String repository = root.get("repository").asText();
            String prNumber = root.get("pull_request_number").asText();

            log.info("Consuming completed review for PR #{} in repo {}", prNumber, repository);

            StringBuilder markdown = new StringBuilder("### 🤖 AI Code Review Findings\n\n");
            JsonNode comments = root.path("review").path("comments");
            
            if (comments.isArray() && !comments.isEmpty()) {
                for (JsonNode comment : comments) {
                    markdown.append("**File:** `").append(comment.path("file_path").asText()).append("` ")
                            .append("(Line ").append(comment.path("line_number").asText()).append(")\n\n")
                            .append("> ").append(comment.path("body").asText()).append("\n\n---\n\n");
                }
            } else {
                markdown.append("✅ No significant logic, edge-case, or security issues found.");
            }

            postReviewAsBot(repository, prNumber, markdown.toString());

        } catch (Exception e) {
            log.error("Failed to parse Kafka message or post to GitHub", e);
        }
    }

    private void postReviewAsBot(String repository, String prNumber, String markdownBody) throws Exception {
        String[] repoParts = repository.split("/");
        String owner = repoParts[0];
        String repo = repoParts[1];

        String botToken = gitHubAuthService.getInstallationToken(owner, repo);

        String escapedBody = markdownBody.replace("\"", "\\\"").replace("\n", "\\n");
        String bodyJson = "{\"body\": \"" + escapedBody + "\"}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.github.com/repos/" + owner + "/" + repo + "/issues/" + prNumber + "/comments"))
                .header("Authorization", "Bearer " + botToken)
                .header("Accept", "application/vnd.github+json")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(bodyJson))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 201) {
            log.info("Successfully published AI review back to GitHub PR #{} as lens[bot]", prNumber);
        } else {
            log.error("GitHub API rejected comment. Status: {}, Response: {}", response.statusCode(), response.body());
        }
    }
}