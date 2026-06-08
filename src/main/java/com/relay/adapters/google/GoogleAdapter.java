package com.relay.adapters.google;

import com.relay.adapters.PlatformAdapter;
import com.relay.adapters.model.AdapterError;
import com.relay.adapters.model.CanonicalBrief;
import com.relay.adapters.model.CapabilityMatrix;
import com.relay.adapters.model.DeploymentResult;
import com.relay.shared.Objective;
import com.relay.shared.Platform;
import java.util.EnumSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Google Ads adapter. Spine implementation simulates a Performance Max creation and derives a
 * deterministic native id from the idempotency key. Real Google Ads API mapping lands in phase 2.
 */
@Component
public class GoogleAdapter implements PlatformAdapter {

    private static final Logger log = LoggerFactory.getLogger(GoogleAdapter.class);

    @Override
    public Platform platform() {
        return Platform.GOOGLE;
    }

    @Override
    public CapabilityMatrix capabilities() {
        // Google supports calls (call ads) and the rest, but not social "engagement".
        return new CapabilityMatrix(EnumSet.of(
            Objective.AWARENESS, Objective.TRAFFIC, Objective.LEADS,
            Objective.APP_PROMOTION, Objective.SALES, Objective.CALLS));
    }

    @Override
    public DeploymentResult submit(CanonicalBrief brief, String idemKey) {
        log.info("[google] submit campaign={} objective={} idem={}",
            brief.name(), brief.objective(), idemKey);
        String extId = "google_pmax_" + Integer.toHexString(idemKey.hashCode());
        return new DeploymentResult(extId, "ENABLED");
    }

    @Override
    public void pause(String extCampaignId) {
        log.info("[google] pause {}", extCampaignId);
    }

    @Override
    public void resume(String extCampaignId) {
        log.info("[google] resume {}", extCampaignId);
    }

    @Override
    public AdapterError classifyError(Throwable t) {
        return new AdapterError(AdapterError.Kind.RETRYABLE, t.getMessage());
    }
}
