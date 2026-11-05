package com.teamanalyzer.teamanalyzer.infra.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.client.RestClient;

import com.teamanalyzer.teamanalyzer.port.EmailSender;

/** Exactly one {@link EmailSender} is active for every supported mail configuration. */
class EmailSenderSelectionTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withBean(JavaMailSender.class, () -> mock(JavaMailSender.class))
            .withBean(RestClient.Builder.class, RestClient::builder)
            .withUserConfiguration(DisabledEmailSender.class, SpringMailSenderAdapter.class,
                    BrevoApiEmailSenderAdapter.class);

    @Test
    void mail_disabled_by_default_uses_the_no_op_sender() {
        runner.run(ctx -> assertThat(ctx).getBean(EmailSender.class).isInstanceOf(DisabledEmailSender.class));
    }

    @Test
    void enabled_without_provider_uses_smtp() {
        runner.withPropertyValues("app.mail.enabled=true", "app.mail.from=team@example.org")
                .run(ctx -> assertThat(ctx).getBean(EmailSender.class).isInstanceOf(SpringMailSenderAdapter.class));
    }

    @Test
    void smtp_without_sender_address_fails_at_startup() {
        runner.withPropertyValues("app.mail.enabled=true", "app.mail.provider=smtp")
                .run(ctx -> assertThat(ctx).hasFailed());
    }

    @Test
    void enabled_with_brevo_api_uses_the_brevo_adapter() {
        runner.withPropertyValues("app.mail.enabled=true", "app.mail.provider=brevo-api",
                "app.mail.api-key=test-key", "app.mail.from=team@example.org")
                .run(ctx -> assertThat(ctx).getBean(EmailSender.class)
                        .isInstanceOf(BrevoApiEmailSenderAdapter.class));
    }

    @Test
    void brevo_without_api_key_fails_at_startup() {
        runner.withPropertyValues("app.mail.enabled=true", "app.mail.provider=brevo-api", "app.mail.from=team@example.org")
                .run(ctx -> assertThat(ctx).hasFailed());
    }

    @Test
    void disabled_wins_even_when_a_provider_is_configured() {
        runner.withPropertyValues("app.mail.enabled=false", "app.mail.provider=brevo-api")
                .run(ctx -> assertThat(ctx).getBean(EmailSender.class).isInstanceOf(DisabledEmailSender.class));
    }
}
