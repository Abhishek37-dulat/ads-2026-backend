package com.relay.deployment;

import com.relay.shared.Platform;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeploymentService {

    private final PlatformDeploymentRepository repo;

    public DeploymentService(PlatformDeploymentRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public void markSubmitting(UUID deploymentId) {
        repo.findById(deploymentId).ifPresent(d -> d.setStatus("submitting"));
    }

    @Transactional
    public void markLive(UUID deploymentId, String extCampaignId) {
        repo.findById(deploymentId).ifPresent(d -> {
            d.setStatus("live");
            d.setExtCampaignId(extCampaignId);
            d.setLastError(null);
        });
    }

    @Transactional
    public void markFailed(UUID deploymentId, String message) {
        repo.findById(deploymentId).ifPresent(d -> {
            d.setStatus("failed");
            d.setLastError(Map.of("message", message == null ? "unknown" : message));
        });
    }

    @Transactional
    public void pauseLive(UUID campaignId, List<Platform> platforms) {
        for (PlatformDeployment d : repo.findByCampaignId(campaignId)) {
            if (platforms.contains(d.getPlatform()) && "live".equals(d.getStatus())) {
                d.setStatus("paused");
            }
        }
    }

    @Transactional(readOnly = true)
    public List<PlatformDeployment> forCampaign(UUID campaignId) {
        return repo.findByCampaignId(campaignId);
    }
}
