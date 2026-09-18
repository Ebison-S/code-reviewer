package com.lens.apiservice.client;

import com.lens.apiservice.dto.ReviewResultPayload;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class GitHubClient {
    private final RestClient restClient;

    public GitHubClient(@Value("${github.api.token}") String token) {
        this.restClient = RestClient.builder()
            .baseUrl("https://api.github.com")
            .defaultHeader("Authorization", "Bearer " + token)
            .defaultHeader("Accept", "application/vnd.github.v3+json")
            .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
            .build();
    }

    public void postReviewComment(ReviewResultPayload payload) {
        // In GitHub's API, every PR is also an Issue, making this the correct endpoint for general PR comments
        String url = "/repos/" + payload.repository() + "/issues/" + payload.pullRequestNumber() + "/comments";
        
        StringBuilder markdown = new StringBuilder("### 🤖 Lens AI Reviewer\n\n");
        if (payload.review().comments().isEmpty()) {
            markdown.append("✅ No significant logic, edge-case, or security issues found.");
        } else {
            payload.review().comments().forEach(comment -> 
                markdown.append("**File:** `").append(comment.filePath())
                        .append("` (Line ").append(comment.lineNumber()).append(")\n")
                        .append("> ").append(comment.body()).append("\n\n")
            );
        }

        restClient.post()
            .uri(url)
            .body(Map.of("body", markdown.toString()))
            .retrieve()
            .toBodilessEntity();
    }
}