package com.relay.adapters;

import com.relay.adapters.model.CapabilityMatrix;
import com.relay.shared.Platform;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** Looks up the adapter for a platform and exposes the combined capability matrix to the wizard. */
@Component
public class AdapterRegistry {

    private final Map<Platform, PlatformAdapter> byPlatform;

    public AdapterRegistry(List<PlatformAdapter> adapters) {
        this.byPlatform = adapters.stream()
            .collect(Collectors.toMap(PlatformAdapter::platform, Function.identity()));
    }

    public PlatformAdapter get(Platform platform) {
        PlatformAdapter adapter = byPlatform.get(platform);
        if (adapter == null) {
            throw new IllegalArgumentException("No adapter registered for " + platform);
        }
        return adapter;
    }

    public List<Platform> platforms() {
        return byPlatform.keySet().stream().sorted().toList();
    }

    public Map<Platform, CapabilityMatrix> capabilities() {
        return byPlatform.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().capabilities()));
    }
}
