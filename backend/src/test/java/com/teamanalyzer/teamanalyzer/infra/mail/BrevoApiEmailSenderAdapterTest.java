package com.teamanalyzer.teamanalyzer.infra.mail;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mail.MailSendException;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class BrevoApiEmailSenderAdapterTest {

  @Test
  void sendsTransactionalEmailUsingBrevoApiKeyAndConfiguredSender() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    var adapter = new BrevoApiEmailSenderAdapter(
        builder, "test-api-key", "team@example.org", "Team Analyzer");

    server.expect(requestTo("https://api.brevo.com/v3/smtp/email"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("api-key", "test-api-key"))
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(content().json("""
            {
              "sender": {"name": "Team Analyzer", "email": "team@example.org"},
              "to": [{"email": "recipient@example.org"}],
              "subject": "Verify your email",
              "textContent": "Open the verification link."
            }
            """))
        .andRespond(withSuccess());

    adapter.send("recipient@example.org", "Verify your email", "Open the verification link.");

    server.verify();
  }

  @Test
  void refusesToStartWithoutApiKeyOrSender() {
    assertThrows(IllegalStateException.class, () -> new BrevoApiEmailSenderAdapter(
        RestClient.builder(), "", "team@example.org", "Team Analyzer"));
    assertThrows(IllegalStateException.class, () -> new BrevoApiEmailSenderAdapter(
        RestClient.builder(), "test-api-key", " ", "Team Analyzer"));
  }

  @Test
  void reportsRejectedRequestsAsMailSendException() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    var adapter = new BrevoApiEmailSenderAdapter(builder, "test-api-key", "team@example.org", "Team Analyzer");
    server.expect(requestTo("https://api.brevo.com/v3/smtp/email"))
        .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

    assertThrows(MailSendException.class,
        () -> adapter.send("recipient@example.org", "Subject", "Body"));
  }
}
