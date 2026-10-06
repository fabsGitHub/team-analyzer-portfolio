// backend/src/main/java/com/teamanalyzer/teamanalyzer/infra/crypto/JdkDigestService.java
package com.teamanalyzer.teamanalyzer.infra.crypto;

import com.teamanalyzer.teamanalyzer.port.DigestService;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class JdkDigestService implements DigestService {
    @Override
    public byte[] sha256(String input) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
