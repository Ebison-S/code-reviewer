package com.lens.apiservice.dto;

import java.util.List;

public record ReviewData(
    List<CodeComment> comments
) {}