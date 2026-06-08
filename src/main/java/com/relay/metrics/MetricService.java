package com.relay.metrics;

import com.relay.metrics.dto.MetricRollup;
import com.relay.shared.WorkspaceContext;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Reads/writes the ClickHouse {@code metric_snapshot} store. ClickHouse has no RLS, so every
 * query is explicitly scoped to the request's workspace.
 */
@Service
public class MetricService {

    private final JdbcTemplate ch;

    public MetricService(JdbcTemplate clickHouseJdbc) {
        this.ch = clickHouseJdbc;
    }

    public void write(UUID workspaceId, UUID campaignId, UUID deploymentId, String platform,
                      LocalDateTime hour, long impressions, long clicks, double spend,
                      long conversions, double revenue) {
        ch.update(
            "INSERT INTO relay.metric_snapshot "
                + "(workspace_id, deployment_id, campaign_id, platform, ts, impressions, clicks, "
                + "spend, conversions, revenue) VALUES (?,?,?,?,?,?,?,?,?,?)",
            workspaceId, deploymentId, campaignId, platform, hour,
            impressions, clicks, spend, conversions, revenue);
    }

    /** Per-platform totals for the current workspace, optionally filtered to one campaign. */
    public List<MetricRollup> rollupByPlatform(UUID campaignId) {
        UUID ws = WorkspaceContext.require();
        StringBuilder sql = new StringBuilder(
            "SELECT platform, sum(impressions), sum(clicks), sum(spend), sum(conversions), sum(revenue) "
                + "FROM relay.metric_snapshot WHERE workspace_id = ?");
        Object[] args;
        if (campaignId != null) {
            sql.append(" AND campaign_id = ?");
            args = new Object[]{ws, campaignId};
        } else {
            args = new Object[]{ws};
        }
        sql.append(" GROUP BY platform ORDER BY platform");
        return ch.query(sql.toString(), (rs, n) -> new MetricRollup(
            rs.getString(1), rs.getLong(2), rs.getLong(3), rs.getBigDecimal(4),
            rs.getLong(5), rs.getBigDecimal(6)), args);
    }
}
