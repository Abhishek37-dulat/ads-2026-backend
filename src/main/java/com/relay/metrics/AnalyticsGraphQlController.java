package com.relay.metrics;

import com.relay.metrics.dto.MetricRollup;
import java.util.List;
import java.util.UUID;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/** GraphQL read side for the analytics dashboard. */
@Controller
public class AnalyticsGraphQlController {

    private final MetricService metrics;

    public AnalyticsGraphQlController(MetricService metrics) {
        this.metrics = metrics;
    }

    @QueryMapping
    public List<MetricRollup> analytics(@Argument UUID campaignId) {
        return metrics.rollupByPlatform(campaignId);
    }
}
