package com.lens.apiservice.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

@Slf4j
@Component
public class GitHubSignatureValidator implements WebhookSignatureValidator {

    private final String webhookSecret;

    public GitHubSignatureValidator (@Value ("${github.webhook.secret}") String webhookSecret) {
        this.webhookSecret = webhookSecret;
    }

    @Override
    public boolean isValid (String payload, String signatureHeader) {
        if (signatureHeader == null || !signatureHeader.startsWith ("sha256=")) {
            log.warn ("Missing or malformed signature header");
            return false;
        }
        try {
            String expectedSignature = "sha256=" + calculateHmac256 (payload, webhookSecret);
            return expectedSignature.equals (signatureHeader);
        } catch (Exception e) {
            log.error ("Failed to calculate HMAC signature", e);
            return false;
        }
    }

    private String calculateHmac256 (String data, String key) throws Exception {
        Mac mac = Mac.getInstance ("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec (key.getBytes (StandardCharsets.UTF_8), "HmacSHA256");
        mac.init (secretKeySpec);
        return HexFormat.of ().formatHex (mac.doFinal (data.getBytes (StandardCharsets.UTF_8)));
    }
}