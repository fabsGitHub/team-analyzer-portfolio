package com.teamanalyzer.teamanalyzer.infra.mail;

import com.teamanalyzer.teamanalyzer.port.EmailSender;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.mail.provider", havingValue = "smtp", matchIfMissing = true)
public class SpringMailSenderAdapter implements EmailSender {
  private final JavaMailSender delegate;

  public SpringMailSenderAdapter(JavaMailSender delegate) {
    this.delegate = delegate;
  }

  @Override
  public void send(String to, String subject, String text) {
    SimpleMailMessage msg = new SimpleMailMessage();
    msg.setTo(to);
    msg.setSubject(subject);
    msg.setText(text);
    delegate.send(msg);
  }
}
