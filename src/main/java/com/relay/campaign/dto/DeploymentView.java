package com.relay.campaign.dto;

import com.relay.shared.Platform;
import java.util.UUID;

public record DeploymentView(UUID id, Platform platform, String status, String extCampaignId) {}
