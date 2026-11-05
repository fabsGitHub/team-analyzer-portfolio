package com.teamanalyzer.teamanalyzer.infra.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.teamanalyzer.teamanalyzer.port.EmailSender;

/**
 * Used when {@code app.mail.enabled=false} (local development and tests). Messages are
 * dropped; only the recipient and subject are logged so one-time links never reach the log.
 */
@Component
@ConditionalOnProperty(prefix = "app.mail", name = "enabled", havingValue = "false", matchIfMissing = true)
public class DisabledEmailSender implements EmailSender {

  private static final Logger log = LoggerFactory.getLogger(DisabledEmailSender.class);

  @Override
  public void send(String to, String subject, String text) {
    log.info("Mail delivery is disabled; skipped message '{}' to {}", subject, to);
  }
}
