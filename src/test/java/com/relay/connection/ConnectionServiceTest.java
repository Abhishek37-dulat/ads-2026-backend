package com.relay.connection;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;

class ConnectionServiceTest {

    @Test
    void platformConnectionsStayDisabledUntilProviderOauthExists() {
        ConnectionService service = new ConnectionService(
            mock(ConnectionRepository.class), mock(VerificationCheckRepository.class));

        assertThrows(PlatformConnectionUnavailableException.class, service::connect);
    }
}
