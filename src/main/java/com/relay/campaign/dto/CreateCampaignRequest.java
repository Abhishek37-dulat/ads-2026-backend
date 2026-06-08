package com.relay.campaign.dto;

import com.relay.shared.Objective;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Create a draft canonical campaign (POST /v1/campaigns). */
public record CreateCampaignRequest(
    @NotBlank String name,
    @NotNull Objective objective,
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
