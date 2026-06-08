package com.relay.campaign;

import com.relay.shared.Objective;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * The canonical campaign brief — platform-agnostic. Adapters translate it into each platform's
 * native shape; {@code PlatformDeployment} records the concrete result per platform.
 */
@Entity
@Table(name = "campaign")
@Getter
@Setter
public class Campaign {

    @Id
    private UUID id;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Objective objective;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private Map<String, Object> destination = new HashMap<>();

    @Column(nullable = false)
    private String status = "draft";

    @Column(name = "budget_mode", nullable = false)
    private String budgetMode = "daily";

    @Column(name = "budget_amount", nullable = false)
    private BigDecimal budgetAmount = BigDecimal.ZERO;

    @Column(nullable = false)
    private String split = "auto";

    @Column(name = "all_or_nothing", nullable = false)
    private boolean allOrNothing = false;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        OffsetDateTime now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
