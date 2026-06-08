package com.relay.connection;

import com.relay.connection.dto.ConnectionView;
import com.relay.connection.dto.VerificationView;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ConnectionService {

    private final ConnectionRepository connections;
    private final VerificationCheckRepository checks;

    public ConnectionService(ConnectionRepository connections, VerificationCheckRepository checks) {
        this.connections = connections;
        this.checks = checks;
    }

    public List<ConnectionView> listWithHealth() {
        return connections.findAllByOrderByPlatformAsc().stream()
            .map(c -> new ConnectionView(
                c.getId(),
                c.getPlatform(),
                c.getAccountName(),
                c.getStatus(),
                checks.findByConnectionId(c.getId()).stream()
                    .map(v -> new VerificationView(v.getKind(), v.getStatus(), v.getDetail()))
                    .toList()))
            .toList();
    }
}
