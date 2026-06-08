package com.relay.shared.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Boot 4 / Spring Framework 7 default to Jackson 3 ({@code tools.jackson}) for MVC, so the
 * only auto-configured {@code ObjectMapper} bean is the Jackson 3 type. A few components
 * (ComplianceEngine rule-pack parsing, the WebSocket frame encoder) use the Jackson 2
 * {@code com.fasterxml.jackson} API, which is still on the classpath — this provides that bean.
 */
@Configuration
public class JacksonConfig {

    @Bean
    ObjectMapper jackson2ObjectMapper() {
        return new ObjectMapper();
    }
}
