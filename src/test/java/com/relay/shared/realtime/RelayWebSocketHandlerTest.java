package com.relay.shared.realtime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.relay.auth.JwtService;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

class RelayWebSocketHandlerTest {

    @Test
    void broadcastsOnlyToAuthenticatedSessionsInTheTargetWorkspace() throws Exception {
        JwtService jwt = new JwtService(
            "test-secret-that-is-definitely-at-least-thirty-two-characters", 1);
        RelayWebSocketHandler handler = new RelayWebSocketHandler(new ObjectMapper(), jwt);
        UUID firstWorkspace = UUID.randomUUID();
        UUID secondWorkspace = UUID.randomUUID();
        WebSocketSession first = session();
        WebSocketSession second = session();

        authenticate(handler, first, jwt.issue(UUID.randomUUID(), "one@example.com", "One",
            firstWorkspace, "One Workspace", false));
        authenticate(handler, second, jwt.issue(UUID.randomUUID(), "two@example.com", "Two",
            secondWorkspace, "Two Workspace", false));

        handler.broadcast(firstWorkspace, "launch", Map.of("status", "live"));

        verify(first, times(2)).sendMessage(any(TextMessage.class));
        verify(second, times(1)).sendMessage(any(TextMessage.class));
    }

    private static WebSocketSession session() {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getAttributes()).thenReturn(new HashMap<>());
        when(session.isOpen()).thenReturn(true);
        return session;
    }

    private static void authenticate(RelayWebSocketHandler handler, WebSocketSession session,
                                     String token) throws Exception {
        handler.handleTextMessage(session,
            new TextMessage("{\"type\":\"auth\",\"token\":\"" + token + "\"}"));
    }
}
