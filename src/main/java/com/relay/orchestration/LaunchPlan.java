package com.relay.orchestration;

import com.relay.adapters.model.CanonicalBrief;
import com.relay.shared.Platform;
import java.util.List;
import java.util.UUID;

/** Input to {@link LaunchWorkflow}: the brief, the per-platform deployment plan, and the policy. */
public record LaunchPlan(
    UUID campaignId,
    UUID workspaceId,
    boolean allOrNothing,
    CanonicalBrief brief,
    List<Platform> platforms,
    List<Item> deployments) {

    /** One platform's slice of the launch — the deployment row to drive + idempotency key. */
    public record Item(UUID deploymentId, Platform platform, String idemKey) {}
}
