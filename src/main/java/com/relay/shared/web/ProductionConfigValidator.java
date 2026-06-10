package com.relay.shared.web;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.context.EnvironmentAware;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** Fails production startup before Flyway or other singleton beans run when settings are unsafe. */
@Component
@Profile("prod")
public class ProductionConfigValidator implements BeanFactoryPostProcessor, EnvironmentAware {

    private Environment env;

    @Override
    public void setEnvironment(Environment env) {
        this.env = env;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        validate();
    }

    void validate() {
        List<String> errors = new ArrayList<>();
        requireSecret(errors, "relay.auth.jwt-secret", 32);
        requireSecret(errors, "spring.datasource.password", 16);
        requireSecret(errors, "relay.clickhouse.password", 16);
        requireSecret(errors, "relay.s3.secret-key", 16);
        requireSecret(errors, "relay.admin.password", 16);
        require(errors, "relay.admin.email");
        require(errors, "relay.google.client-id");
        requireSecret(errors, "relay.google.client-secret", 16);
        require(errors, "spring.mail.host");
        require(errors, "spring.mail.username");
        requireSecret(errors, "spring.mail.password", 8);
        require(errors, "relay.mail.from");
        requireSecret(errors, "relay.sms.fast2sms-key", 8);

        String baseUrl = env.getProperty("relay.app.base-url", "");
        try {
            if (!"https".equalsIgnoreCase(URI.create(baseUrl).getScheme())) {
                errors.add("relay.app.base-url must use https");
            }
        } catch (Exception e) {
            errors.add("relay.app.base-url must be a valid HTTPS URL");
        }

        String origins = env.getProperty("relay.cors.allowed-origins", "");
        if (!StringUtils.hasText(origins)
            || Arrays.stream(origins.split(","))
                .map(String::trim)
                .anyMatch(origin -> !origin.startsWith("https://") || origin.contains("localhost"))) {
            errors.add("relay.cors.allowed-origins must contain only production HTTPS origins");
        }

        if (!errors.isEmpty()) {
            throw new IllegalStateException("Unsafe production configuration: "
                + String.join("; ", errors));
        }
    }

    private void require(List<String> errors, String property) {
        if (!StringUtils.hasText(env.getProperty(property))) {
            errors.add(property + " is required");
        }
    }

    private void requireSecret(List<String> errors, String property, int minLength) {
        String value = env.getProperty(property, "");
        if (value.length() < minLength || value.contains("relay-dev-secret")
            || value.equals("admin12345") || value.equals("relay-secret")) {
            errors.add(property + " must be a strong secret of at least " + minLength + " characters");
        }
    }
}
