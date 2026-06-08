package com.relay.connection;

import com.relay.connection.dto.ConnectionView;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Connection + verification health for the workspace (tech-design.html §08). */
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

    /** Begin OAuth link for a platform account. Stubbed for the spine — returns an auth URL. */
    @PostMapping("/{platform}/oauth")
    public Map<String, String> beginOauth(@PathVariable String platform) {
        return Map.of(
            "platform", platform,
            "authUrl", "https://oauth.relay.dev/" + platform.toLowerCase() + "/start",
            "status", "redirect");
    }
}
