package com.relay.campaign.dto;

import com.relay.shared.Objective;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Partial update of a brief (PATCH /v1/campaigns/{id}). Null fields are left unchanged. */
public record PatchCampaignRequest(
    String name,
    Objective objective,
    Map<String, Object> destination,
    String budgetMode,
    BigDecimal budgetAmount,
    String split,
    Boolean allOrNothing,
    String headline,
    String body,
    List<String> geo,
    Integer ageMin,
    Integer ageMax,
    List<String> audiences,
    List<String> interests) {}
