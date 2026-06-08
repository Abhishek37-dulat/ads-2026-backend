package com.relay.campaign.dto;

import com.relay.shared.Objective;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record CampaignDetail(
    UUID id,
    String name,
    Objective objective,
    String status,
    Map<String, Object> destination,
    String budgetMode,
    BigDecimal budgetAmount,
    String split,
    boolean allOrNothing,
    List<String> geo,
    Integer ageMin,
    Integer ageMax,
    List<String> audiences,
    List<String> interests,
    List<DeploymentView> deployments) {}
