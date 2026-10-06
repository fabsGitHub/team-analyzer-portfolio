// backend/src/main/java/com/teamanalyzer/teamanalyzer/service/DownloadTokenService.java
package com.teamanalyzer.teamanalyzer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teamanalyzer.teamanalyzer.port.AppClock;
import com.teamanalyzer.teamanalyzer.port.TokenSigner;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

@Service
public class DownloadTokenService {
  private final ObjectMapper om;
  private final TokenSigner signer;
  private final AppClock clock;

  public DownloadTokenService(ObjectMapper om, TokenSigner signer, AppClock clock) {
    this.om = om;
    this.signer = signer;
    this.clock = clock;
  }

  public String issue(UUID surveyId, UUID userId, Duration ttl) {
    try {
      long exp = clock.now().plus(ttl).getEpochSecond();
      String json = om.writeValueAsString(Map.of(
          "sid", surveyId.toString(), "uid", userId.toString(), "exp", exp));
      String payload = Base64.getUrlEncoder().withoutPadding()
          .encodeToString(json.getBytes(StandardCharsets.UTF_8));
      String sig = signer.signUrlSafe(payload);
      return payload + "." + sig;
    } catch (Exception e) {
      throw new IllegalStateException("token issue failed", e);
    }
  }

  public UUID verifyAndExtractUser(String token, UUID expectedSurveyId) {
    try {
      String[] parts = token.split("\\.");
      if (parts.length != 2)
        throw new IllegalArgumentException("malformed");
      String payload = parts[0], sig = parts[1];
      if (!signer.matches(payload, sig))
        throw new IllegalArgumentException("bad signature");

      byte[] raw = Base64.getUrlDecoder().decode(payload);
      var n = om.readTree(new String(raw, StandardCharsets.UTF_8));

      if (!expectedSurveyId.toString().equals(n.path("sid").asText()))
        throw new IllegalArgumentException("wrong sid");
      if (clock.now().getEpochSecond() > n.path("exp").asLong())
        throw new IllegalArgumentException("expired");
      return UUID.fromString(n.path("uid").asText());
    } catch (IllegalArgumentException ex) {
      throw ex;
    } catch (Exception e) {
      throw new IllegalArgumentException("invalid token", e);
    }
  }
}
