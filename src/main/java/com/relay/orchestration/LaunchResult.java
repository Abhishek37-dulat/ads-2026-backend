package com.relay.orchestration;

import com.relay.shared.Platform;
import java.util.Map;

/** Terminal outcome of a launch: per-platform status and whether the brief was rejected. */
public record LaunchResult(boolean rejected, Map<Platform, String> statuses) {}
