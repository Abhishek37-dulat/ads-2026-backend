package com.relay.connection;

import com.relay.connection.dto.ConnectionView;
import com.relay.shared.Platform;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Connection + verification management for the workspace (tech-design.html §08). */
@RestController
@RequestMapping("/v1/connections")
public class ConnectionController {

    private final ConnectionService service;

    public ConnectionController(ConnectionService service) {
        this.service = service;
    }

    @GetMapping
    public List<ConnectionView> list() {
        return service.listWithHealth();
    }

    /** Link a platform account — persists a real connection scoped to the workspace. */
    @PostMapping
    public ResponseEntity<ConnectionView> connect(@RequestBody ConnectRequest req) {
        ConnectionView v = service.connect(req.platform(), req.accountName(), req.extAccountId());
        return ResponseEntity.status(HttpStatus.CREATED).body(v);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> disconnect(@PathVariable UUID id) {
        service.disconnect(id);
        return ResponseEntity.noContent().build();
    }

    public record ConnectRequest(Platform platform, String accountName, String extAccountId) {}
}
