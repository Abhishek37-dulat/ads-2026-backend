package com.relay.connection.dto;

import com.relay.shared.Platform;
import java.util.List;
import java.util.UUID;

public record ConnectionView(
    UUID id,
    Platform platform,
    String accountName,
    String status,
    List<VerificationView> checks) {}
