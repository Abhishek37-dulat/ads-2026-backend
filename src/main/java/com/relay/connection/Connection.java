package com.relay.connection;

import com.relay.shared.Platform;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/** A linked platform ad-account. Verification checks hang off it (connections health screen). */
@Entity
@Table(name = "connection")
@Getter
@Setter
public class Connection {

    @Id
    private UUID id;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Platform platform;

    @Column(name = "account_name", nullable = false)
    private String accountName;

    @Column(name = "ext_account_id")
    private String extAccountId;

    @Column(nullable = false)
    private String status = "disconnected";

    @Column(name = "created_at")
    private OffsetDateTime createdAt;
}
