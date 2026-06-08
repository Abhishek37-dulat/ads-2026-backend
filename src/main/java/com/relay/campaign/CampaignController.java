package com.relay.campaign;

import com.relay.campaign.dto.CampaignDetail;
import com.relay.campaign.dto.CampaignSummary;
import com.relay.campaign.dto.CreateCampaignRequest;
import com.relay.campaign.dto.FindingView;
import com.relay.campaign.dto.LaunchRequest;
import com.relay.campaign.dto.LaunchResponse;
import com.relay.campaign.dto.PatchCampaignRequest;
import com.relay.shared.Platform;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Campaign commands (REST) — tech-design.html §08. */
@RestController
@RequestMapping("/v1/campaigns")
public class CampaignController {

    private final CampaignService service;

    public CampaignController(CampaignService service) {
        this.service = service;
    }

    @GetMapping
    public List<CampaignSummary> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    public CampaignDetail get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PostMapping
    public ResponseEntity<CampaignDetail> create(@Valid @RequestBody CreateCampaignRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
    }

    @PatchMapping("/{id}")
    public CampaignDetail patch(@PathVariable UUID id, @RequestBody PatchCampaignRequest req) {
        return service.patch(id, req);
    }

    /** Run compliance, return findings (non-mutating). */
    @PostMapping("/{id}/preflight")
    public List<FindingView> preflight(@PathVariable UUID id, @RequestBody PreflightRequest req) {
        return service.preflight(id, req.platforms());
    }

    /** Start the durable launch workflow → 202 + workflowId. */
    @PostMapping("/{id}/launch")
    public ResponseEntity<LaunchResponse> launch(@PathVariable UUID id, @RequestBody LaunchRequest req) {
        return ResponseEntity.accepted().body(service.launch(id, req));
    }

    @PostMapping("/{id}/pause")
    public ResponseEntity<Void> pause(@PathVariable UUID id) {
        service.pause(id);
        return ResponseEntity.accepted().build();
    }

    public record PreflightRequest(List<Platform> platforms) {}
}
