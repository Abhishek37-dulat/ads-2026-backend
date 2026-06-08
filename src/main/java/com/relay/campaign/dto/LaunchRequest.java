package com.relay.campaign.dto;

import com.relay.shared.Platform;
import java.util.List;

/** Launch a campaign across the given platforms (POST /v1/campaigns/{id}/launch). */
public record LaunchRequest(List<Platform> platforms, String idempotencyKey) {}
