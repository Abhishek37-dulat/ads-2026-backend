package com.relay.metrics;

import com.relay.deployment.PlatformDeployment;
import com.relay.deployment.PlatformDeploymentRepository;
import com.relay.metrics.dto.MetricRollup;
import com.relay.shared.WorkspaceContext;
import com.relay.shared.realtime.RelayWebSocketHandler;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Unified cross-platform analytics (reads ClickHouse rollups). */
@RestController
@RequestMapping("/v1/analytics")
public class AnalyticsController {

    private final MetricService metrics;
    private final PlatformDeploymentRepository deployments;
    private final RelayWebSocketHandler realtime;

    public AnalyticsController(MetricService metrics, PlatformDeploymentRepository deployments,
                              RelayWebSocketHandler realtime) {
        this.metrics = metrics;
        this.deployments = deployments;
        this.realtime = realtime;
    }

    @GetMapping
    public List<MetricRollup> rollup(@RequestParam(required = false) UUID campaignId) {
        return metrics.rollupByPlatform(campaignId);
    }

    /**
     * Dev helper: generate 24h of sample metrics for a campaign's deployments so the analytics
     * screen has data. In production these flow from platform webhooks/pollers (phase 3).
     */
    @PostMapping("/seed")
    @Transactional(readOnly = true)
    public Map<String, Object> seed(@RequestParam UUID campaignId) {
        UUID ws = WorkspaceContext.require();
        List<PlatformDeployment> deps = deployments.findByCampaignId(campaignId);
        int rows = 0;
        for (PlatformDeployment d : deps) {
            for (int h = 23; h >= 0; h--) {
                LocalDateTime hour = LocalDateTime.now().minusHours(h).withMinute(0).withSecond(0).withNano(0);
                long impressions = ThreadLocalRandom.current().nextLong(800, 5000);
                long clicks = (long) (impressions * ThreadLocalRandom.current().nextDouble(0.01, 0.06));
                double spend = clicks * ThreadLocalRandom.current().nextDouble(0.4, 2.2);
                long conversions = (long) (clicks * ThreadLocalRandom.current().nextDouble(0.02, 0.12));
                double revenue = conversions * ThreadLocalRandom.current().nextDouble(20, 120);
                metrics.write(ws, campaignId, d.getId(), d.getPlatform().name(), hour,
                    impressions, clicks, spend, conversions, revenue);
                rows++;
            }
        }
        realtime.broadcast(ws, "metrics", Map.of("campaignId", campaignId.toString(), "rows", rows));
        return Map.of("seeded", rows, "deployments", deps.size());
    }
}
