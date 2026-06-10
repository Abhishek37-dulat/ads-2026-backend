package com.relay.auth;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    @Test
    void rotatingSigningSecretInvalidatesOldTokens() {
        JwtService oldSigner = new JwtService(
            "old-signing-secret-that-is-definitely-long-enough", 7);
        String token = oldSigner.issue(UUID.randomUUID(), "user@example.com", "User",
            UUID.randomUUID(), "User Workspace", false);
        JwtService rotatedSigner = new JwtService(
            "new-signing-secret-that-is-definitely-long-enough", 7);

        assertThrows(Exception.class, () -> rotatedSigner.parse(token));
    }
}
