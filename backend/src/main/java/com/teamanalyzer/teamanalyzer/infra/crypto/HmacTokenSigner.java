// backend/src/main/java/com/teamanalyzer/teamanalyzer/infra/crypto/HmacTokenSigner.java
package com.teamanalyzer.teamanalyzer.infra.crypto;

import com.teamanalyzer.teamanalyzer.port.TokenSigner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.nio.charset.StandardCharsets;

@Component
public class HmacTokenSigner implements TokenSigner {
    private final Mac mac;

    public HmacTokenSigner(@Value("${app.download-token-secret}") String secret) {
        try {
            mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        } catch (Exception e) {
            throw new IllegalStateException("Cannot init HMAC", e);
        }
    }

    @Override
    public String signUrlSafe(String payloadB64) {
        byte[] s = mac.doFinal(payloadB64.getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(s);
    }

    @Override
    public boolean matches(String payloadB64, String expected) {
        return signUrlSafe(payloadB64).equals(expected);
    }
}
