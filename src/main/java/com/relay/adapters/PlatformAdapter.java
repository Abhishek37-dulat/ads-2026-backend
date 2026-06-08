package com.relay.adapters;

import com.relay.adapters.model.AdapterError;
import com.relay.adapters.model.CanonicalBrief;
import com.relay.adapters.model.CapabilityMatrix;
import com.relay.adapters.model.DeploymentResult;
import com.relay.shared.Platform;

/**
 * The single extension point of Relay. One implementation per platform; adding a platform is
 * additive (this package only) and never ripples into core.
 *
 * <p>Mirrors tech-design.html §07 (PlatformAdapter interface).
 */
public interface PlatformAdapter {

    Platform platform();

    /** Declares supported objectives/surfaces — feeds the wizard's capability matrix. */
    CapabilityMatrix capabilities();

    /** Translate the canonical brief → native create call. Idempotent on {@code idemKey}. */
    DeploymentResult submit(CanonicalBrief brief, String idemKey);

    void pause(String extCampaignId);

    void resume(String extCampaignId);

    /** Normalize a native error into Relay's retry-classified taxonomy. */
    AdapterError classifyError(Throwable t);
}
