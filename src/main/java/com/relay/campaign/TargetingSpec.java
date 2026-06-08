package com.relay.campaign;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Canonical targeting, 1:1 with a campaign, expressed in Relay's taxonomy. */
@Entity
@Table(name = "targeting_spec")
@Getter
@Setter
public class TargetingSpec {

    @Id
    private UUID id;

    @Column(name = "campaign_id", nullable = false)
    private UUID campaignId;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private List<String> geo = new ArrayList<>();

    @Column(name = "age_min")
    private Integer ageMin;

    @Column(name = "age_max")
    private Integer ageMax;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private List<String> audiences = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private List<String> interests = new ArrayList<>();
}
