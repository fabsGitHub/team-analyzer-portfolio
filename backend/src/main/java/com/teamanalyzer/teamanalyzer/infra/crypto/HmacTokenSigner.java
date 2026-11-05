package com.teamanalyzer.teamanalyzer.infra.crypto;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.teamanalyzer.teamanalyzer.port.TokenSigner;

/**
 * HMAC-SHA256 signer for download links.
 *
 * <p>{@link Mac} instances are not thread-safe, so a fresh instance is created per
 * signature. Signature comparison is constant-time.
 */
@Component
public class HmacTokenSigner implements TokenSigner {

    private static final String ALGORITHM = "HmacSHA256";
    private static final int MIN_SECRET_BYTES = 32;

    private final SecretKeySpec key;

    public HmacTokenSigner(@Value("${app.download-token-secret}") String secret) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "app.download-token-secret must contain at least " + MIN_SECRET_BYTES + " bytes");
        }
        this.key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM);
        newMac(); // fail fast if the algorithm is unavailable
    }

    @Override
    public String signUrlSafe(String payloadB64) {
        byte[] signature = newMac().doFinal(payloadB64.getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
    }

    @Override
    public boolean matches(String payloadB64, String expected) {
        if (payloadB64 == null || expected == null) {
            return false;
        }
        byte[] actual = signUrlSafe(payloadB64).getBytes(StandardCharsets.US_ASCII);
        return MessageDigest.isEqual(actual, expected.getBytes(StandardCharsets.US_ASCII));
    }

    private Mac newMac() {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(key);
            return mac;
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Cannot initialise " + ALGORITHM, e);
        }
    }
}
