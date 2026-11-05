package com.teamanalyzer.teamanalyzer.service;

import org.springframework.stereotype.Service;

import com.teamanalyzer.teamanalyzer.port.EmailSender;

/** Application-level entry point for outgoing mail; delegates to the configured {@link EmailSender}. */
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
