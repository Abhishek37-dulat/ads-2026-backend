package com.relay.adapters.model;

/**
 * Native platform errors normalized into a retry-classified taxonomy. Drives the launch saga's
 * retry vs. fail-fast decision.
 */
public record AdapterError(Kind kind, String message) {

    public enum Kind {
        RETRYABLE,   // 429 / 5xx / timeout — retry with backoff
        POLICY,      // creative/policy rejection — non-retryable
        AUTH,        // invalid/expired credentials — non-retryable
        FATAL        // anything else unrecoverable
    }

    public boolean retryable() {
        return kind == Kind.RETRYABLE;
    }
}
