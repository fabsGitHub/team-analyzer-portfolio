package com.teamanalyzer.teamanalyzer.infra.mail;

import org.junit.jupiter.api.Test;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SpringMailSenderAdapterTest {

  @Test
  void sendsWithTheConfiguredVerifiedSenderAddress() {
    JavaMailSender delegate = mock(JavaMailSender.class);
    SpringMailSenderAdapter adapter = new SpringMailSenderAdapter(delegate, "team@example.org");

    adapter.send("recipient@example.org", "Verify your email", "Open the verification link.");

    var message = org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
    verify(delegate).send(message.capture());
    assertEquals("team@example.org", message.getValue().getFrom());
    assertEquals("recipient@example.org", message.getValue().getTo()[0]);
    assertEquals("Verify your email", message.getValue().getSubject());
    assertEquals("Open the verification link.", message.getValue().getText());
  }
}
