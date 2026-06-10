package com.relay.connection;

import com.relay.connection.dto.ConnectionView;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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

    /** Platform account linking remains disabled until provider OAuth is implemented. */
    @PostMapping
    public ResponseEntity<ConnectionView> connect() {
        return ResponseEntity.ok(service.connect());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> disconnect(@PathVariable UUID id) {
        service.disconnect(id);
        return ResponseEntity.noContent().build();
    }
}
