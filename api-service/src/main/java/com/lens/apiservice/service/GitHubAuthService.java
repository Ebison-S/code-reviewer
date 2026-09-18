package com.lens.apiservice.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Date;

@Service
public class GitHubAuthService {

    @Value("${github.app.id}")
    private String appId;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String getInstallationToken(String owner, String repo) throws Exception {
        InputStream is = getClass().getResourceAsStream("/github-app.pem");
        if (is == null) throw new RuntimeException("github-app.pem not found in resources!");
        
        String pem = new String(is.readAllBytes(), StandardCharsets.UTF_8)
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");

        byte[] pkcs8Bytes = Base64.getDecoder().decode(pem);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        PrivateKey privateKey = kf.generatePrivate(new PKCS8EncodedKeySpec(pkcs8Bytes));

        long now = System.currentTimeMillis();
        String jwt = Jwts.builder()
                .setIssuer(appId)
                .setIssuedAt(new Date(now - 60000)) 
                .setExpiration(new Date(now + (9 * 60 * 1000))) 
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();

        HttpRequest instRequest = HttpRequest.newBuilder()
                .uri(URI.create("https://api.github.com/repos/" + owner + "/" + repo + "/installation"))
                .header("Authorization", "Bearer " + jwt)
                .header("Accept", "application/vnd.github+json")
                .GET()
                .build();

        HttpResponse<String> instResponse = httpClient.send(instRequest, HttpResponse.BodyHandlers.ofString());
        String installationId = objectMapper.readTree(instResponse.body()).get("id").asText();

        HttpRequest tokenRequest = HttpRequest.newBuilder()
                .uri(URI.create("https://api.github.com/app/installations/" + installationId + "/access_tokens"))
                .header("Authorization", "Bearer " + jwt)
                .header("Accept", "application/vnd.github+json")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> tokenResponse = httpClient.send(tokenRequest, HttpResponse.BodyHandlers.ofString());
        return objectMapper.readTree(tokenResponse.body()).get("token").asText(); 
    }
}