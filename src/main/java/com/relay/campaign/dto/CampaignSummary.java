package com.relay.campaign.dto;

import com.relay.shared.Objective;
import com.relay.shared.Platform;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CampaignSummary(
    UUID id,
    String name,
    Objective objective,
    String status,
    BigDecimal budgetAmount,
    String budgetMode,
    int platformCount,
    List<Platform> platforms) {}
