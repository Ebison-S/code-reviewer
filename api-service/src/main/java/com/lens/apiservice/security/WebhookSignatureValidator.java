package com.lens.apiservice.security;

public interface WebhookSignatureValidator {
    boolean isValid (String payload, String signatureHeader);
}