package com.relay.adapters;

import com.relay.shared.Objective;
import com.relay.shared.Platform;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Objective × platform capability matrix. The wizard greys out impossible combinations and the
 * compliance screen renders the full grid.
 */
@RestController
@RequestMapping("/v1/capabilities")
public class CapabilityController {

    private final AdapterRegistry registry;

    public CapabilityController(AdapterRegistry registry) {
        this.registry = registry;
    }

    /** { platform: [objective, …] } for every registered adapter. */
    @GetMapping
    public Map<Platform, List<Objective>> matrix() {
        return registry.capabilities().entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> e.getValue().supportedObjectives().stream().sorted().toList()));
    }
}
