package com.relay.shared;

import java.util.UUID;

/**
 * Holds the current request's workspace id in a thread-local so the JPA layer can set
 * {@code app.workspace} for Postgres Row-Level Security. Set by {@code WorkspaceFilter},
 * cleared at the end of each request.
 */
public final class WorkspaceContext {

    private static final ThreadLocal<UUID> CURRENT = new ThreadLocal<>();

    private WorkspaceContext() {}

    public static void set(UUID workspaceId) {
        CURRENT.set(workspaceId);
    }

    public static UUID get() {
        return CURRENT.get();
    }

    public static UUID require() {
        UUID id = CURRENT.get();
        if (id == null) {
            throw new IllegalStateException("No workspace bound to the current request");
        }
        return id;
    }

    public static void clear() {
        CURRENT.remove();
    }
}
