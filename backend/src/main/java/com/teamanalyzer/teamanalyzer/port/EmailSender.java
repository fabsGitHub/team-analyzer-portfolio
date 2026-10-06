package com.teamanalyzer.teamanalyzer.port;

public interface EmailSender {
    void send(String to, String subject, String text);
}