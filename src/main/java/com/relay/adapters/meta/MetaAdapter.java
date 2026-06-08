package com.relay.adapters.meta;

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
 * Meta (Facebook/Instagram) adapter. Spine implementation simulates the Marketing API call and
 * derives a deterministic native id from the idempotency key, so a retried submit reuses the
 * same native campaign rather than creating a duplicate. Real DTO mapping (MapStruct) lands in
 * phase 2.
 */
@Component
public class MetaAdapter implements PlatformAdapter {

    private static final Logger log = LoggerFactory.getLogger(MetaAdapter.class);

    @Override
    public Platform platform() {
        return Platform.META;
    }

    @Override
    public CapabilityMatrix capabilities() {
        // Meta supports most objectives; not native click-to-call.
        return new CapabilityMatrix(EnumSet.of(
            Objective.AWARENESS, Objective.TRAFFIC, Objective.ENGAGEMENT,
            Objective.LEADS, Objective.APP_PROMOTION, Objective.SALES));
    }

    @Override
    public DeploymentResult submit(CanonicalBrief brief, String idemKey) {
        log.info("[meta] submit campaign={} objective={} idem={}",
            brief.name(), brief.objective(), idemKey);
        String extId = "act_meta_" + Integer.toHexString(idemKey.hashCode());
        return new DeploymentResult(extId, "ACTIVE");
    }

    @Override
    public void pause(String extCampaignId) {
        log.info("[meta] pause {}", extCampaignId);
    }

    @Override
    public void resume(String extCampaignId) {
        log.info("[meta] resume {}", extCampaignId);
    }

    @Override
    public AdapterError classifyError(Throwable t) {
        return new AdapterError(AdapterError.Kind.RETRYABLE, t.getMessage());
    }
}
