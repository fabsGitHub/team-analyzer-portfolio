package com.teamanalyzer.teamanalyzer.infra.mail;

import com.teamanalyzer.teamanalyzer.port.EmailSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.MediaType;
import org.springframework.mail.MailSendException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

/**
 * Sends plain-text mail through Brevo's transactional email API (HTTPS), which works on hosts
 * that block outbound SMTP. Active when mail is enabled and {@code app.mail.provider=brevo-api}.
 */
@Component
@ConditionalOnExpression("${app.mail.enabled:false} and '${app.mail.provider:smtp}' == 'brevo-api'")
public class BrevoApiEmailSenderAdapter implements EmailSender {
  private static final String API_BASE_URL = "https://api.brevo.com/v3";

  private final RestClient restClient;
  private final String apiKey;
  private final String fromAddress;
  private final String fromName;

  public BrevoApiEmailSenderAdapter(
      RestClient.Builder restClientBuilder,
      @Value("${app.mail.api-key:}") String apiKey,
      @Value("${app.mail.from:}") String fromAddress,
      @Value("${app.mail.from-name:Team Analyzer}") String fromName) {
    // Fail at startup instead of silently dropping every verification and reset mail.
    if (apiKey == null || apiKey.isBlank()) {
      throw new IllegalStateException("BREVO_API_KEY (app.mail.api-key) must be set when app.mail.provider=brevo-api");
    }
    if (fromAddress == null || fromAddress.isBlank()) {
      throw new IllegalStateException("SPRING_MAIL_FROM (app.mail.from) must be a verified Brevo sender address");
    }
    this.apiKey = apiKey;
    this.fromAddress = fromAddress;
    this.fromName = fromName;
    this.restClient = restClientBuilder
        .baseUrl(API_BASE_URL)
        .defaultHeader("api-key", apiKey)
        .defaultHeader("accept", MediaType.APPLICATION_JSON_VALUE)
        .build();
  }

  @Override
  public void send(String to, String subject, String text) {
    Map<String, Object> payload = Map.of(
        "sender", Map.of("name", fromName, "email", fromAddress),
        "to", List.of(Map.of("email", to)),
        "subject", subject,
        "textContent", text);

    try {
      restClient.post()
          .uri("/smtp/email")
          .contentType(MediaType.APPLICATION_JSON)
          .body(payload)
          .retrieve()
          .toBodilessEntity();
    } catch (RestClientResponseException ex) {
      throw new MailSendException("Brevo rejected the email request (HTTP "
          + ex.getStatusCode().value() + ").");
    } catch (RestClientException ex) {
      throw new MailSendException("The Brevo email request could not be completed.");
    }
  }
}
