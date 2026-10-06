// backend/src/main/java/com/teamanalyzer/teamanalyzer/port/DigestService.java
package com.teamanalyzer.teamanalyzer.port;

public interface DigestService {
    byte[] sha256(String input);
}
