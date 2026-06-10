package com.relay.orchestration;

import com.relay.adapters.AdapterRegistry;
import com.relay.adapters.model.AdapterError;
import com.relay.adapters.model.DeploymentResult;
import com.relay.compliance.ComplianceEngine;
import com.relay.compliance.Finding;
import com.relay.deployment.DeploymentService;
import com.relay.shared.Platform;
import com.relay.shared.WorkspaceContext;
import com.relay.shared.realtime.RelayWebSocketHandler;
import io.temporal.failure.ApplicationFailure;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Spring-managed activity implementations. Each method binds the plan's workspace to
 * {@link WorkspaceContext} so the transactional DB writes pass Postgres RLS on worker threads,
 * and clears it in a finally to avoid leaking across pooled worker threads.
 */
@Component
public class LaunchActivitiesImpl implements LaunchActivities {

    private static final Logger log = LoggerFactory.getLogger(LaunchActivitiesImpl.class);

    private final ComplianceEngine compliance;
    private final AdapterRegistry adapters;
    private final DeploymentService deployments;
    private final RelayWebSocketHandler realtime;

    public LaunchActivitiesImpl(ComplianceEngine compliance, AdapterRegistry adapters,
                                DeploymentService deployments, RelayWebSocketHandler realtime) {
        this.compliance = compliance;
        this.adapters = adapters;
        this.deployments = deployments;
        this.realtime = realtime;
    }

    @Override
    public boolean preflightHasBlocking(LaunchPlan plan) {
        List<Finding> findings = compliance.evaluate(plan.brief(), plan.platforms());
        boolean blocking = compliance.hasBlocking(findings);
        realtime.broadcast(plan.workspaceId(), "launch", Map.of(
            "campaignId", plan.campaignId().toString(),
            "phase", "preflight",
            "blocking", blocking,
            "findings", findings.size()));
        return blocking;
    }

    @Override
    public String submitPlatform(LaunchPlan.Item item, LaunchPlan plan) {
        WorkspaceContext.set(plan.workspaceId());
        try {
            deployments.markSubmitting(item.deploymentId());
            broadcast(plan.workspaceId(), plan.campaignId(), item.platform(), "submitting", null);
            try {
                DeploymentResult result =
                    adapters.get(item.platform()).submit(plan.brief(), item.idemKey());
                deployments.markLive(item.deploymentId(), result.extCampaignId());
                broadcast(plan.workspaceId(), plan.campaignId(), item.platform(), "live", result.extCampaignId());
                return "live";
            } catch (Exception ex) {
                AdapterError error = adapters.get(item.platform()).classifyError(ex);
                deployments.markFailed(item.deploymentId(), error.message());
                broadcast(plan.workspaceId(), plan.campaignId(), item.platform(), "failed", error.message());
                // map non-retryable kinds to the workflow's doNotRetry types
                String type = switch (error.kind()) {
                    case POLICY -> "POLICY_REJECTED";
                    case AUTH -> "INVALID_CREDS";
                    default -> "ADAPTER_ERROR";
                };
                throw ApplicationFailure.newFailure(error.message(), type);
            }
        } finally {
            WorkspaceContext.clear();
        }
    }

    @Override
    public void pausePlatforms(List<Platform> platforms, UUID campaignId, UUID workspaceId) {
        WorkspaceContext.set(workspaceId);
        try {
            deployments.pauseLive(campaignId, platforms);
            log.info("[compensation] paused live siblings {} for campaign {}", platforms, campaignId);
        } finally {
            WorkspaceContext.clear();
        }
    }

    private void broadcast(UUID workspaceId, UUID campaignId, Platform platform, String status, String detail) {
        var payload = new java.util.HashMap<String, Object>();
        payload.put("campaignId", campaignId.toString());
        payload.put("platform", platform.name());
        payload.put("status", status);
        if (detail != null) {
            payload.put("detail", detail);
        }
        realtime.broadcast(workspaceId, "launch", payload);
    }
}
