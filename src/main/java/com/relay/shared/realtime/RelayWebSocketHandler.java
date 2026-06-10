package com.relay.shared.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.relay.auth.JwtService;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * Authenticated WebSocket fan-out for {@code /v1/stream}. A client must send
 * {@code {"type":"auth","token":"..."}} before it receives workspace-scoped events.
 */
@Component
public class RelayWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(RelayWebSocketHandler.class);

    private final Set<WebSocketSession> sessions = new CopyOnWriteArraySet<>();
    private final ObjectMapper mapper;
    private final JwtService jwt;

    public RelayWebSocketHandler(ObjectMapper mapper, JwtService jwt) {
        this.mapper = mapper;
        this.jwt = jwt;
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> body = mapper.readValue(message.getPayload(), Map.class);
            if (!"auth".equals(body.get("type")) || !(body.get("token") instanceof String token)) {
                throw new IllegalArgumentException("Authentication frame required");
            }
            UUID workspaceId = UUID.fromString(jwt.parse(token).get("workspace", String.class));
            session.getAttributes().put("workspaceId", workspaceId);
            synchronized (session) {
                session.sendMessage(new TextMessage("{\"type\":\"authenticated\"}"));
            }
            sessions.add(session);
        } catch (Exception e) {
            session.close(CloseStatus.POLICY_VIOLATION);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
    }

    /** Broadcast an event only to authenticated clients in the target workspace. */
    public void broadcast(UUID workspaceId, String topic, Map<String, Object> payload) {
        try {
            var frame = new java.util.HashMap<String, Object>(payload);
            frame.put("topic", topic);
            TextMessage message = new TextMessage(mapper.writeValueAsBytes(frame));
            for (WebSocketSession s : sessions) {
                if (s.isOpen() && workspaceId.equals(s.getAttributes().get("workspaceId"))) {
                    synchronized (s) {
                        s.sendMessage(message);
                    }
                }
            }
        } catch (IOException e) {
            log.warn("WebSocket broadcast failed: {}", e.getMessage());
        }
    }
}
