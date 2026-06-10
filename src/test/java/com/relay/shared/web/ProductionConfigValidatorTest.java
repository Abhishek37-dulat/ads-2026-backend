package com.relay.shared.web;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class ProductionConfigValidatorTest {

    @Test
    void rejectsDevelopmentDefaults() {
        MockEnvironment env = validEnvironment()
            .withProperty("relay.app.base-url", "http://localhost:8081");

        ProductionConfigValidator validator = validator(env);
        assertThrows(IllegalStateException.class,
            validator::validate);
    }

    @Test
    void acceptsCompleteHttpsProductionConfiguration() {
        assertDoesNotThrow(() -> validator(validEnvironment()).validate());
    }

    private static MockEnvironment validEnvironment() {
        return new MockEnvironment()
            .withProperty("relay.auth.jwt-secret", "jwt-secret-that-is-definitely-over-thirty-two-characters")
            .withProperty("spring.datasource.password", "postgres-secret-strong")
            .withProperty("relay.clickhouse.password", "clickhouse-secret-strong")
            .withProperty("relay.s3.secret-key", "minio-secret-strong")
            .withProperty("relay.admin.password", "admin-secret-strong")
            .withProperty("relay.admin.email", "ops@example.com")
            .withProperty("relay.google.client-id", "google-client-id")
            .withProperty("relay.google.client-secret", "google-client-secret-strong")
            .withProperty("spring.mail.host", "smtp.example.com")
            .withProperty("spring.mail.username", "smtp-user")
            .withProperty("spring.mail.password", "smtp-password")
            .withProperty("relay.mail.from", "no-reply@example.com")
            .withProperty("relay.sms.fast2sms-key", "fast2sms-key")
            .withProperty("relay.app.base-url", "https://d0187.in")
            .withProperty("relay.cors.allowed-origins",
                "https://d0187.in,https://www.d0187.in,https://admin.d0187.in");
    }

    private static ProductionConfigValidator validator(MockEnvironment env) {
        ProductionConfigValidator validator = new ProductionConfigValidator();
        validator.setEnvironment(env);
        return validator;
    }
}
