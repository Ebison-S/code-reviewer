package com.lens.apiservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CodeComment(
    @JsonProperty("file_path") String filePath,
    @JsonProperty("line_number") Integer lineNumber,
    String body
) {}