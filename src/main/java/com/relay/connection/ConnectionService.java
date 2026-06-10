package com.relay.connection;

import com.relay.connection.dto.ConnectionView;
import com.relay.connection.dto.VerificationView;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.UUID;
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

    public ConnectionView connect() {
        throw new PlatformConnectionUnavailableException();
    }

    @Transactional
    public void disconnect(UUID id) {
        Connection c = connections.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Connection not found"));
        checks.deleteAll(checks.findByConnectionId(c.getId()));
        connections.delete(c);
    }

}
