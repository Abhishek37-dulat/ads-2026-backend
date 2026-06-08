package com.relay.auth;

import java.util.UUID;

/** Authenticated user attached to the SecurityContext for the request. */
public record AuthPrincipal(UUID userId, String email, String name, UUID workspaceId, String workspaceName, boolean admin) {}
