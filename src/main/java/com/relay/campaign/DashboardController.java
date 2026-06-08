package com.relay.campaign;

import com.relay.connection.ConnectionRepository;
import com.relay.deployment.PlatformDeploymentRepository;
import com.relay.metrics.MetricService;
import com.relay.metrics.dto.MetricRollup;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Command Center summary stats for the home dashboard. */
@RestController
@RequestMapping("/v1/dashboard")
public class DashboardController {

    private final CampaignRepository campaigns;
    private final PlatformDeploymentRepository deployments;
    private final ConnectionRepository connections;
    private final MetricService metrics;

    public DashboardController(CampaignRepository campaigns, PlatformDeploymentRepository deployments,
                              ConnectionRepository connections, MetricService metrics) {
        this.campaigns = campaigns;
        this.deployments = deployments;
        this.connections = connections;
        this.metrics = metrics;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public Map<String, Object> stats() {
        long campaignCount = campaigns.count();
        long liveDeployments = deployments.findAll().stream()
            .filter(d -> "live".equals(d.getStatus())).count();
        long connected = connections.findAll().stream()
            .filter(c -> "connected".equals(c.getStatus())).count();
        long totalConnections = connections.count();

        List<MetricRollup> rollups = metrics.rollupByPlatform(null);
        BigDecimal spend = rollups.stream()
            .map(MetricRollup::spend).filter(java.util.Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        long conversions = rollups.stream().mapToLong(MetricRollup::conversions).sum();
        BigDecimal revenue = rollups.stream()
            .map(MetricRollup::revenue).filter(java.util.Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return Map.of(
            "campaigns", campaignCount,
            "liveDeployments", liveDeployments,
            "connectionsConnected", connected,
            "connectionsTotal", totalConnections,
            "spend", spend,
            "conversions", conversions,
            "revenue", revenue);
    }
}
