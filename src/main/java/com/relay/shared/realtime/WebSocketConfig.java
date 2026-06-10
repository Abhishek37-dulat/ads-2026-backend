package com.relay.shared.realtime;

import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/** Registers the realtime stream at {@code /v1/stream?topics=launch,metrics}. */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final RelayWebSocketHandler handler;
    private final List<String> allowedOrigins;

    public WebSocketConfig(RelayWebSocketHandler handler,
                           @Value("${relay.cors.allowed-origins}") List<String> allowedOrigins) {
        this.handler = handler;
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/v1/stream")
            .setAllowedOrigins(allowedOrigins.toArray(String[]::new));
    }
}
