package com.teamanalyzer.teamanalyzer;

import java.security.SecureRandom;
import java.util.Base64;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
@org.springframework.test.context.ActiveProfiles("test")
class TeamanalyzerApplicationTests {
  private static final String TEST_JWT_SECRET_BASE64 = createTestSecret();
  private static final String TEST_EMAIL_VERIFY_SECRET = createTestSecret();
  private static final String TEST_DOWNLOAD_TOKEN_SECRET = createTestSecret();

  @DynamicPropertySource
  static void configureTestSigningProperties(DynamicPropertyRegistry registry) {
    registry.add("app.jwt.secret", () -> TEST_JWT_SECRET_BASE64);
    registry.add("app.auth.hmac-secret", () -> TEST_EMAIL_VERIFY_SECRET);
    registry.add("app.download-token-secret", () -> TEST_DOWNLOAD_TOKEN_SECRET);
  }

  private static String createTestSecret() {
    byte[] secret = new byte[64];
    new SecureRandom().nextBytes(secret);
    return Base64.getEncoder().encodeToString(secret);
  }

  @Test
  void contextLoads() {
  }
}
