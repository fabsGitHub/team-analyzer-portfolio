// backend/src/main/java/com/teamanalyzer/teamanalyzer/infra/security/BcryptPasswordHasher.java
package com.teamanalyzer.teamanalyzer.infra.security;

import com.teamanalyzer.teamanalyzer.port.PasswordHasher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class BcryptPasswordHasher implements PasswordHasher {
    private final PasswordEncoder encoder;

    public BcryptPasswordHasher(PasswordEncoder encoder) {
        this.encoder = encoder;
    }

    @Override
    public String hash(String raw) {
        return encoder.encode(raw);
    }
}
