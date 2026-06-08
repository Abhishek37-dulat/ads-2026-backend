package com.relay.shared.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * Raw WebSocket fan-out for {@code /v1/stream}. Broadcasts launch + metric events to every
 * connected client as JSON {@code {topic, ...payload}} frames. The Next.js client uses the
 * native browser WebSocket and filters by topic.
 */
@Component
public class RelayWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(RelayWebSocketHandler.class);

    private final Set<WebSocketSession> sessions = new CopyOnWriteArraySet<>();
    private final ObjectMapper mapper;

    public RelayWebSocketHandler(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
    }

    /** Broadcast an event under a topic (e.g. "launch", "metrics") to all connected clients. */
    public void broadcast(String topic, Map<String, Object> payload) {
        try {
            var frame = new java.util.HashMap<String, Object>(payload);
            frame.put("topic", topic);
            TextMessage message = new TextMessage(mapper.writeValueAsBytes(frame));
            for (WebSocketSession s : sessions) {
                if (s.isOpen()) {
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
