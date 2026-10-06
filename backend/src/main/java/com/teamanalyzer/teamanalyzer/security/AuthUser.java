package com.teamanalyzer.teamanalyzer.security;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

public record AuthUser(
        UUID userId,
        String email,
        List<String> roles) implements Principal {
    @Override
    public String getName() {
        return email;
    }
}
