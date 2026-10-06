// backend/src/main/java/com/teamanalyzer/teamanalyzer/port/TokenSigner.java
package com.teamanalyzer.teamanalyzer.port;

public interface TokenSigner {
    String signUrlSafe(String base64UrlPayload);

    boolean matches(String base64UrlPayload, String expectedSignature);
}
