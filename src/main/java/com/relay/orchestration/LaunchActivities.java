package com.relay.orchestration;

import com.relay.shared.Platform;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;
import java.util.List;

/**
 * Durable activities the launch saga schedules. Each runs on a worker, is retried independently,
 * and (for submit) is idempotent on the deployment id.
 */
@ActivityInterface
public interface LaunchActivities {

    /** Compliance pre-flight gate. Returns true if any finding is block-severity. */
    @ActivityMethod
    boolean preflightHasBlocking(LaunchPlan plan);

    /** Submit one platform via its adapter and flip the deployment row. Returns final status. */
    @ActivityMethod
    String submitPlatform(LaunchPlan.Item item, LaunchPlan plan);

    /** Compensation for all-or-nothing campaigns: pause already-live siblings. */
    @ActivityMethod
    void pausePlatforms(List<Platform> platforms, java.util.UUID campaignId, java.util.UUID workspaceId);
}
