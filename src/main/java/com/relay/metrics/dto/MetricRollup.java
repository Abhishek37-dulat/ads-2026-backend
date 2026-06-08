package com.relay.metrics.dto;

import java.math.BigDecimal;

/** Aggregated metrics for one platform (unified analytics screen). */
public record MetricRollup(
    String platform,
    long impressions,
    long clicks,
    BigDecimal spend,
    long conversions,
    BigDecimal revenue) {}
