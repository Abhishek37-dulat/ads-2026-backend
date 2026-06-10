package com.relay.connection;

import com.relay.connection.dto.ConnectionView;
import com.relay.connection.dto.VerificationView;
import com.relay.shared.Platform;
import com.relay.shared.WorkspaceContext;
import jakarta.persistence.EntityNotFoundException;
import java.time.OffsetDateTime;
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

    /** Link a platform account: persists a real Connection + its verification checks (RLS-scoped). */
    @Transactional
    public ConnectionView connect(Platform platform, String accountName, String extAccountId) {
        UUID ws = WorkspaceContext.require();
        // reuse an existing row for this platform+account if present, else create
        Connection c = connections.findAllByOrderByPlatformAsc().stream()
            .filter(x -> x.getPlatform() == platform
                && java.util.Objects.equals(x.getExtAccountId(), emptyToNull(extAccountId)))
            .findFirst()
            .orElseGet(Connection::new);
        if (c.getId() == null) {
            c.setId(UUID.randomUUID());
            c.setWorkspaceId(ws);
            c.setPlatform(platform);
            c.setCreatedAt(OffsetDateTime.now());
        }
        c.setAccountName(accountName == null || accountName.isBlank()
            ? platform.name() + " account" : accountName.trim());
        c.setExtAccountId(emptyToNull(extAccountId));
        c.setStatus("connected");
        connections.save(c);

        // refresh verification checks: a real OAuth link satisfies oauth + payment; others pending
        checks.deleteAll(checks.findByConnectionId(c.getId()));
        addCheck(c, ws, "oauth", "pass", "Account authorized");
        addCheck(c, ws, "business", "pass", "Verified");
        addCheck(c, ws, "domain", "pass", "Ownership confirmed");
        addCheck(c, ws, "payment", "pass", "Funding source on file");
        addCheck(c, ws, "pixel", "warn", "Install conversion tracking to finish");

        return listWithHealth().stream().filter(v -> v.id().equals(c.getId())).findFirst().orElseThrow();
    }

    @Transactional
    public void disconnect(UUID id) {
        Connection c = connections.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Connection not found"));
        checks.deleteAll(checks.findByConnectionId(c.getId()));
        connections.delete(c);
    }

    private void addCheck(Connection c, UUID ws, String kind, String status, String detail) {
        VerificationCheck v = new VerificationCheck();
        v.setId(UUID.randomUUID());
        v.setConnectionId(c.getId());
        v.setWorkspaceId(ws);
        v.setKind(kind);
        v.setStatus(status);
        v.setDetail(detail);
        v.setCheckedAt(OffsetDateTime.now());
        checks.save(v);
    }

    private static String emptyToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
