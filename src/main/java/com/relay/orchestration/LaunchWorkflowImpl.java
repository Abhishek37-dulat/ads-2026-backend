package com.relay.orchestration;

import com.relay.shared.Platform;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Async;
import io.temporal.workflow.Promise;
import io.temporal.workflow.Workflow;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Durable launch saga (tech-design.html §06.1). Pre-flight is a gate; platforms fan out in
 * parallel; failures are isolated so a sibling failing never fails the others; optional
 * compensation pauses live siblings for all-or-nothing campaigns.
 */
public class LaunchWorkflowImpl implements LaunchWorkflow {

    private final LaunchActivities activities = Workflow.newActivityStub(
        LaunchActivities.class,
        ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofSeconds(90))
            .setRetryOptions(RetryOptions.newBuilder()
                .setBackoffCoefficient(2.0)
                .setMaximumAttempts(5)
                // policy rejections and bad creds are non-retryable — fail that platform fast
                .setDoNotRetry("POLICY_REJECTED", "INVALID_CREDS")
                .build())
            .build());

    @Override
    public LaunchResult launch(LaunchPlan plan) {
        // 1. Pre-flight gate — block-severity findings abort the whole launch.
        if (activities.preflightHasBlocking(plan)) {
            return new LaunchResult(true, Map.of());
        }

        // 2. Fan out — one activity per platform, in parallel.
        Map<Platform, Promise<String>> futures = new LinkedHashMap<>();
        for (LaunchPlan.Item item : plan.deployments()) {
            futures.put(item.platform(),
                Async.function(activities::submitPlatform, item, plan));
        }

        // 3. Collect — failures isolated and recorded; siblings continue.
        Map<Platform, String> statuses = new HashMap<>();
        List<Platform> live = new ArrayList<>();
        for (Map.Entry<Platform, Promise<String>> e : futures.entrySet()) {
            try {
                String status = e.getValue().get();
                statuses.put(e.getKey(), status);
                if ("live".equals(status)) {
                    live.add(e.getKey());
                }
            } catch (Exception ex) {
                statuses.put(e.getKey(), "failed");
            }
        }

        // 4. Optional compensation for all-or-nothing campaigns.
        if (plan.allOrNothing() && statuses.containsValue("failed") && !live.isEmpty()) {
            activities.pausePlatforms(live, plan.campaignId(), plan.workspaceId());
        }

        return new LaunchResult(false, statuses);
    }
}
