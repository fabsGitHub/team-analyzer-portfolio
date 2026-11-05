package com.teamanalyzer.teamanalyzer.support;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Supplies random signing secrets to every test application context, so the repository does not
 * contain key material. One set is generated per test JVM.
 *
 * <p>The values are offered under the environment variable names that {@code application.yml}
 * refers to ({@code ${APP_JWT_SECRET_BASE64}} and so on), with the lowest precedence: real
 * environment variables, test properties and {@code @DynamicPropertySource} still win.
 */
public class TestSigningSecrets implements EnvironmentPostProcessor {

    private static final Map<String, Object> SECRETS = Map.of(
            "APP_JWT_SECRET_BASE64", randomBase64(64),
            "EMAIL_VERIFY_HMAC_SECRET", randomBase64(64),
            "DOWNLOAD_TOKEN_HMAC_SECRET", randomBase64(64));

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        environment.getPropertySources().addLast(new MapPropertySource("testSigningSecrets", SECRETS));
    }

    /** Random bytes, Base64 encoded (also usable where a plain secret of 64+ bytes is needed). */
    public static String randomBase64(int bytes) {
        byte[] secret = new byte[bytes];
        new SecureRandom().nextBytes(secret);
        return Base64.getEncoder().encodeToString(secret);
    }
}
