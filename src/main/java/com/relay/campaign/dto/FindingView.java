package com.relay.campaign.dto;

import com.relay.shared.Platform;

/** Compliance finding surfaced to the wizard's pre-flight step. */
public record FindingView(Platform platform, String ruleCode, String severity, String message) {}
