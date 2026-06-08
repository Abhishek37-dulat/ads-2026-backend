package com.relay.connection;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/** Per-connection health check (oauth/business/domain/payment/pixel) → connections screen. */
@Entity
@Table(name = "verification_check")
@Getter
@Setter
public class VerificationCheck {

    @Id
    private UUID id;

    @Column(name = "connection_id", nullable = false)
    private UUID connectionId;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(nullable = false)
    private String kind;

    @Column(nullable = false)
    private String status;

    @Column
    private String detail;

    @Column(name = "checked_at")
    private OffsetDateTime checkedAt;
}
