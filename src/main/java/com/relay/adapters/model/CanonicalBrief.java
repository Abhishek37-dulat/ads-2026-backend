package com.relay.adapters.model;

import com.relay.shared.Objective;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Platform-agnostic snapshot of a campaign passed to adapters. Adapters map this into each
 * platform's native create/update calls. Decoupled from the JPA entity on purpose.
 */
public record CanonicalBrief(
    UUID campaignId,
    String name,
    Objective objective,
    Map<String, Object> destination,
    String budgetMode,
    BigDecimal budgetAmount,
    BigDecimal budgetShare,
    List<String> geo,
    Integer ageMin,
    Integer ageMax,
    List<String> audiences,
    List<String> interests,
    String headline,
    String body) {}
