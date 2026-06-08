package com.relay.campaign;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CampaignRepository extends JpaRepository<Campaign, UUID> {
    List<Campaign> findAllByOrderByCreatedAtDesc();
}
