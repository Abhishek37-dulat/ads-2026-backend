package com.relay.campaign;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TargetingSpecRepository extends JpaRepository<TargetingSpec, UUID> {
    Optional<TargetingSpec> findByCampaignId(UUID campaignId);
}
