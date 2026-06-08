package com.relay.compliance;

import com.relay.shared.Platform;

/** One rule outcome for one platform. */
public record Finding(Platform platform, String ruleCode, Severity severity, String message) {}
