package com.relay.adapters.model;

import com.relay.shared.Objective;
import java.util.Set;

/**
 * Declares what a platform supports. The wizard queries this to grey out impossible
 * objective×platform combinations before launch (capability matrix screen).
 */
public record CapabilityMatrix(Set<Objective> supportedObjectives) {

    public boolean supports(Objective objective) {
        return supportedObjectives.contains(objective);
    }
}
