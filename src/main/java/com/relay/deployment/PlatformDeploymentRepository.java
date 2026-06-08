package com.relay.deployment;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlatformDeploymentRepository extends JpaRepository<PlatformDeployment, UUID> {
    List<PlatformDeployment> findByCampaignId(UUID campaignId);
}
