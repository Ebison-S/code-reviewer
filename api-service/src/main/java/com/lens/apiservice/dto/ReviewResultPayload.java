package com.lens.apiservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ReviewResultPayload(
    @JsonProperty("pull_request_number") Integer pullRequestNumber,
    String repository,
    ReviewData review
) {}