package com.teamanalyzer.teamanalyzer.service;

import com.teamanalyzer.teamanalyzer.port.EmailSender;
import org.springframework.stereotype.Service;

@Service
public class MailService {
  private final EmailSender sender;

  public MailService(EmailSender sender) {
    this.sender = sender;
  }

  public void send(String to, String subject, String text) {
    sender.send(to, subject, text);
  }
}
