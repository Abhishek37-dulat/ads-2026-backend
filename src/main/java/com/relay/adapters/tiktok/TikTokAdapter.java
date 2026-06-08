package com.relay.adapters.tiktok;

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

/** TikTok adapter stub. Note: no native click-to-call (drives capability grey-out). */
@Component
public class TikTokAdapter implements PlatformAdapter {

    private static final Logger log = LoggerFactory.getLogger(TikTokAdapter.class);

    @Override
    public Platform platform() {
        return Platform.TIKTOK;
    }

    @Override
    public CapabilityMatrix capabilities() {
        return new CapabilityMatrix(EnumSet.of(
            Objective.AWARENESS, Objective.TRAFFIC, Objective.ENGAGEMENT,
            Objective.APP_PROMOTION, Objective.SALES));
    }

    @Override
    public DeploymentResult submit(CanonicalBrief brief, String idemKey) {
        log.info("[tiktok] submit campaign={} idem={}", brief.name(), idemKey);
        return new DeploymentResult("tiktok_" + Integer.toHexString(idemKey.hashCode()), "ENABLE");
    }

    @Override
    public void pause(String extCampaignId) {
        log.info("[tiktok] pause {}", extCampaignId);
    }

    @Override
    public void resume(String extCampaignId) {
        log.info("[tiktok] resume {}", extCampaignId);
    }

    @Override
    public AdapterError classifyError(Throwable t) {
        return new AdapterError(AdapterError.Kind.RETRYABLE, t.getMessage());
    }
}
