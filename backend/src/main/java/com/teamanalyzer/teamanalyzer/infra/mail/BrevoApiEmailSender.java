package com.teamanalyzer.teamanalyzer.infra.mail;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.teamanalyzer.teamanalyzer.port.EmailSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.mail.provider", havingValue = "brevo-api")
public class BrevoApiEmailSender implements EmailSender {
  private static final URI SEND_EMAIL_URI = URI.create("https://api.brevo.com/v3/smtp/email");

  private final HttpClient httpClient = HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(5))
      .build();
  private final ObjectMapper objectMapper;
  private final String apiKey;
  private final String fromEmail;
  private final String fromName;

  public BrevoApiEmailSender(
      ObjectMapper objectMapper,
      @Value("${app.mail.api-key:}") String apiKey,
      @Value("${app.mail.from:}") String fromEmail,
      @Value("${app.mail.from-name:Team Analyzer}") String fromName) {
    this.objectMapper = objectMapper;
    this.apiKey = apiKey;
    this.fromEmail = fromEmail;
    this.fromName = fromName;
  }

  @Override
  public void send(String to, String subject, String text) {
    if (apiKey.isBlank() || fromEmail.isBlank()) {
      throw new IllegalStateException("Brevo API key and verified sender address are required");
    }

    ObjectNode payload = objectMapper.createObjectNode();
    payload.putObject("sender")
        .put("name", fromName)
        .put("email", fromEmail);
    payload.putArray("to").addObject().put("email", to);
    payload.put("subject", subject);
    payload.put("textContent", text);

    final String body;
    try {
      body = objectMapper.writeValueAsString(payload);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Could not encode the transactional email request", exception);
    }

    HttpRequest request = HttpRequest.newBuilder(SEND_EMAIL_URI)
        .timeout(Duration.ofSeconds(10))
        .header("api-key", apiKey)
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(body))
        .build();

    try {
      HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        throw new IllegalStateException("Brevo rejected the transactional email request (HTTP "
            + response.statusCode() + ")");
      }
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Brevo email request was interrupted", exception);
    } catch (IOException exception) {
      throw new IllegalStateException("Could not reach the Brevo email API", exception);
    }
  }
}
