// backend/src/main/java/com/teamanalyzer/teamanalyzer/infra/mail/SpringMailSenderAdapter.java
package com.teamanalyzer.teamanalyzer.infra.mail;

import com.teamanalyzer.teamanalyzer.port.EmailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
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
