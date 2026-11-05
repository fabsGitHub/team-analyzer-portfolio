package com.teamanalyzer.teamanalyzer.infra.mail;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import com.teamanalyzer.teamanalyzer.port.EmailSender;

/**
 * Sends plain-text mail through the SMTP server configured under {@code spring.mail.*}.
 * Active when mail is enabled and {@code app.mail.provider} is {@code smtp} (the default).
 */
@Component
@ConditionalOnExpression("${app.mail.enabled:false} and '${app.mail.provider:smtp}' == 'smtp'")
public class SpringMailSenderAdapter implements EmailSender {

  private final JavaMailSender delegate;
  private final String fromAddress;

  public SpringMailSenderAdapter(JavaMailSender delegate, @Value("${app.mail.from:}") String fromAddress) {
    // Most SMTP relays reject mail without a verified sender; fail at startup, not on every send.
    if (fromAddress == null || fromAddress.isBlank()) {
      throw new IllegalStateException("SPRING_MAIL_FROM (app.mail.from) must be set when mail is enabled");
    }
    this.delegate = delegate;
    this.fromAddress = fromAddress;
  }

  @Override
  public void send(String to, String subject, String text) {
    SimpleMailMessage msg = new SimpleMailMessage();
    msg.setFrom(fromAddress);
    msg.setTo(to);
    msg.setSubject(subject);
    msg.setText(text);
    delegate.send(msg);
  }
}
