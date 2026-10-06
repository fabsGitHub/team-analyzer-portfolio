// backend/src/main/java/com/teamanalyzer/teamanalyzer/port/PasswordHasher.java
package com.teamanalyzer.teamanalyzer.port;

public interface PasswordHasher {
  String hash(String raw);
}
