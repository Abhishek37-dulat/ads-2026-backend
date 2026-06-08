package com.relay.campaign;

import com.relay.adapters.model.CanonicalBrief;
import com.relay.campaign.dto.CampaignDetail;
import com.relay.campaign.dto.CampaignSummary;
import com.relay.campaign.dto.CreateCampaignRequest;
import com.relay.campaign.dto.DeploymentView;
import com.relay.campaign.dto.FindingView;
import com.relay.campaign.dto.LaunchRequest;
import com.relay.campaign.dto.LaunchResponse;
import com.relay.campaign.dto.PatchCampaignRequest;
import com.relay.compliance.ComplianceEngine;
import com.relay.connection.Connection;
import com.relay.connection.ConnectionRepository;
import com.relay.deployment.PlatformDeployment;
import com.relay.deployment.PlatformDeploymentRepository;
import com.relay.orchestration.LaunchPlan;
import com.relay.orchestration.LaunchService;
import com.relay.shared.Platform;
import com.relay.shared.WorkspaceContext;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Campaign orchestration: validates the canonical brief, builds the launch plan (platforms ×
 * budget split), creates deployment rows, and kicks off the durable launch workflow.
 */
@Service
public class CampaignService {

    private final CampaignRepository campaigns;
    private final TargetingSpecRepository targeting;
    private final ConnectionRepository connections;
    private final PlatformDeploymentRepository deployments;
    private final ComplianceEngine compliance;
    private final LaunchService launchService;

    public CampaignService(CampaignRepository campaigns, TargetingSpecRepository targeting,
                           ConnectionRepository connections, PlatformDeploymentRepository deployments,
                           ComplianceEngine compliance, LaunchService launchService) {
        this.campaigns = campaigns;
        this.targeting = targeting;
        this.connections = connections;
        this.deployments = deployments;
        this.compliance = compliance;
        this.launchService = launchService;
    }

    // ---------------------------------------------------------------- reads
    @Transactional(readOnly = true)
    public List<CampaignSummary> list() {
        return campaigns.findAllByOrderByCreatedAtDesc().stream()
            .map(c -> {
                var deps = deployments.findByCampaignId(c.getId());
                var platforms = deps.stream()
                    .map(com.relay.deployment.PlatformDeployment::getPlatform)
                    .distinct().sorted().toList();
                return new CampaignSummary(c.getId(), c.getName(), c.getObjective(), c.getStatus(),
                    c.getBudgetAmount(), c.getBudgetMode(), deps.size(), platforms);
            })
            .toList();
    }

    @Transactional(readOnly = true)
    public CampaignDetail get(UUID id) {
        Campaign c = require(id);
        TargetingSpec t = targeting.findByCampaignId(id).orElseGet(TargetingSpec::new);
        return new CampaignDetail(c.getId(), c.getName(), c.getObjective(), c.getStatus(),
            c.getDestination(), c.getBudgetMode(), c.getBudgetAmount(), c.getSplit(),
            c.isAllOrNothing(), t.getGeo(), t.getAgeMin(), t.getAgeMax(), t.getAudiences(),
            t.getInterests(), deploymentViews(id));
    }

    // ---------------------------------------------------------------- writes
    @Transactional
    public CampaignDetail create(CreateCampaignRequest req) {
        UUID ws = WorkspaceContext.require();
        Campaign c = new Campaign();
        c.setId(UUID.randomUUID());
        c.setWorkspaceId(ws);
        c.setName(req.name());
        c.setObjective(req.objective());
        c.setDestination(mergeCopy(req.destination(), req.headline(), req.body()));
        if (req.budgetMode() != null) c.setBudgetMode(req.budgetMode());
        if (req.budgetAmount() != null) c.setBudgetAmount(req.budgetAmount());
        if (req.split() != null) c.setSplit(req.split());
        if (req.allOrNothing() != null) c.setAllOrNothing(req.allOrNothing());
        campaigns.save(c);

        TargetingSpec t = new TargetingSpec();
        t.setId(UUID.randomUUID());
        t.setCampaignId(c.getId());
        t.setWorkspaceId(ws);
        if (req.geo() != null) t.setGeo(req.geo());
        t.setAgeMin(req.ageMin());
        t.setAgeMax(req.ageMax());
        if (req.audiences() != null) t.setAudiences(req.audiences());
        if (req.interests() != null) t.setInterests(req.interests());
        targeting.save(t);

        return get(c.getId());
    }

    @Transactional
    public CampaignDetail patch(UUID id, PatchCampaignRequest req) {
        Campaign c = require(id);
        if (req.name() != null) c.setName(req.name());
        if (req.objective() != null) c.setObjective(req.objective());
        if (req.budgetMode() != null) c.setBudgetMode(req.budgetMode());
        if (req.budgetAmount() != null) c.setBudgetAmount(req.budgetAmount());
        if (req.split() != null) c.setSplit(req.split());
        if (req.allOrNothing() != null) c.setAllOrNothing(req.allOrNothing());
        if (req.destination() != null || req.headline() != null || req.body() != null) {
            Map<String, Object> dest = new HashMap<>(c.getDestination());
            if (req.destination() != null) dest.putAll(req.destination());
            if (req.headline() != null) dest.put("headline", req.headline());
            if (req.body() != null) dest.put("body", req.body());
            c.setDestination(dest);
        }

        TargetingSpec t = targeting.findByCampaignId(id).orElseGet(() -> {
            TargetingSpec n = new TargetingSpec();
            n.setId(UUID.randomUUID());
            n.setCampaignId(id);
            n.setWorkspaceId(c.getWorkspaceId());
            return targeting.save(n);
        });
        if (req.geo() != null) t.setGeo(req.geo());
        if (req.ageMin() != null) t.setAgeMin(req.ageMin());
        if (req.ageMax() != null) t.setAgeMax(req.ageMax());
        if (req.audiences() != null) t.setAudiences(req.audiences());
        if (req.interests() != null) t.setInterests(req.interests());

        return get(id);
    }

    // ---------------------------------------------------------------- preflight
    @Transactional(readOnly = true)
    public List<FindingView> preflight(UUID id, List<Platform> platforms) {
        Campaign c = require(id);
        CanonicalBrief brief = buildBrief(c);
        return compliance.evaluate(brief, platforms).stream()
            .map(f -> new FindingView(f.platform(), f.ruleCode(), f.severity().name(), f.message()))
            .toList();
    }

    // ---------------------------------------------------------------- launch
    @Transactional
    public LaunchResponse launch(UUID id, LaunchRequest req) {
        Campaign c = require(id);
        List<Platform> platforms = req.platforms();
        if (platforms == null || platforms.isEmpty()) {
            throw new IllegalArgumentException("At least one platform is required");
        }
        BigDecimal share = BigDecimal.ONE.divide(BigDecimal.valueOf(platforms.size()), 4, RoundingMode.HALF_UP);

        List<LaunchPlan.Item> items = new ArrayList<>();
        for (Platform p : platforms) {
            Connection conn = connections.findAllByOrderByPlatformAsc().stream()
                .filter(x -> x.getPlatform() == p && "connected".equals(x.getStatus()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No connected account for " + p));

            // one deployment per (campaign, connection); reuse on relaunch (idempotent)
            PlatformDeployment dep = deployments.findByCampaignId(id).stream()
                .filter(d -> d.getConnectionId().equals(conn.getId()))
                .findFirst()
                .orElseGet(PlatformDeployment::new);
            if (dep.getId() == null) {
                dep.setId(UUID.randomUUID());
                dep.setWorkspaceId(c.getWorkspaceId());
                dep.setCampaignId(id);
                dep.setConnectionId(conn.getId());
                dep.setPlatform(p);
            }
            dep.setStatus("queued");
            dep.setBudgetShare(share);
            deployments.save(dep);
            items.add(new LaunchPlan.Item(dep.getId(), p, dep.getId().toString()));
        }

        c.setStatus("launching");
        LaunchPlan plan = new LaunchPlan(id, c.getWorkspaceId(), c.isAllOrNothing(),
            buildBrief(c), platforms, items);
        String workflowId = launchService.start(plan);

        return new LaunchResponse(workflowId, deploymentViews(id), "/v1/stream?topics=launch");
    }

    @Transactional
    public void pause(UUID id) {
        Campaign c = require(id);
        c.setStatus("paused");
        deployments.findByCampaignId(id).forEach(d -> {
            if ("live".equals(d.getStatus())) {
                d.setStatus("paused");
            }
        });
    }

    // ---------------------------------------------------------------- helpers
    private Campaign require(UUID id) {
        return campaigns.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Campaign not found: " + id));
    }

    private List<DeploymentView> deploymentViews(UUID campaignId) {
        return deployments.findByCampaignId(campaignId).stream()
            .map(d -> new DeploymentView(d.getId(), d.getPlatform(), d.getStatus(), d.getExtCampaignId()))
            .toList();
    }

    private CanonicalBrief buildBrief(Campaign c) {
        TargetingSpec t = targeting.findByCampaignId(c.getId()).orElseGet(TargetingSpec::new);
        BigDecimal share = BigDecimal.ONE;
        return new CanonicalBrief(c.getId(), c.getName(), c.getObjective(), c.getDestination(),
            c.getBudgetMode(), c.getBudgetAmount(), share, t.getGeo(), t.getAgeMin(), t.getAgeMax(),
            t.getAudiences(), t.getInterests(),
            str(c.getDestination().get("headline")), str(c.getDestination().get("body")));
    }

    private static Map<String, Object> mergeCopy(Map<String, Object> destination, String headline, String body) {
        Map<String, Object> dest = destination == null ? new HashMap<>() : new HashMap<>(destination);
        if (headline != null) dest.put("headline", headline);
        if (body != null) dest.put("body", body);
        return dest;
    }

    private static String str(Object o) {
        return o == null ? null : o.toString();
    }
}
