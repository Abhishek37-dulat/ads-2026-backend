package com.relay.adapters.linkedin;

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

/** LinkedIn adapter stub. B2B-leaning objective support. */
@Component
public class LinkedInAdapter implements PlatformAdapter {

    private static final Logger log = LoggerFactory.getLogger(LinkedInAdapter.class);

    @Override
    public Platform platform() {
        return Platform.LINKEDIN;
    }

    @Override
    public CapabilityMatrix capabilities() {
        return new CapabilityMatrix(EnumSet.of(
            Objective.AWARENESS, Objective.TRAFFIC, Objective.ENGAGEMENT, Objective.LEADS));
    }

    @Override
    public DeploymentResult submit(CanonicalBrief brief, String idemKey) {
        log.info("[linkedin] submit campaign={} idem={}", brief.name(), idemKey);
        return new DeploymentResult("li_" + Integer.toHexString(idemKey.hashCode()), "ACTIVE");
    }

    @Override
    public void pause(String extCampaignId) {
        log.info("[linkedin] pause {}", extCampaignId);
    }

    @Override
    public void resume(String extCampaignId) {
        log.info("[linkedin] resume {}", extCampaignId);
    }

    @Override
    public AdapterError classifyError(Throwable t) {
        return new AdapterError(AdapterError.Kind.RETRYABLE, t.getMessage());
    }
}
