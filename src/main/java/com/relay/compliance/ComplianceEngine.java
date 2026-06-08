package com.relay.compliance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.relay.adapters.AdapterRegistry;
import com.relay.adapters.model.CanonicalBrief;
import com.relay.shared.Objective;
import com.relay.shared.Platform;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

/**
 * Evaluates platform policy rule packs over a canonical brief. Runs both live in the wizard
 * (preflight endpoint) and as the launch workflow's first gate. Rules are data — JSON packs on
 * the classpath ({@code compliance/{platform}.json}) — so policy changes never touch code.
 *
 * <p>An implicit capability rule blocks any objective a platform's adapter doesn't support.
 */
@Service
public class ComplianceEngine {

    private static final Logger log = LoggerFactory.getLogger(ComplianceEngine.class);

    private final ObjectMapper mapper;
    private final AdapterRegistry adapters;
    private final Map<Platform, RulePack> packs = new EnumMap<>(Platform.class);

    public ComplianceEngine(ObjectMapper mapper, AdapterRegistry adapters) {
        this.mapper = mapper;
        this.adapters = adapters;
    }

    @PostConstruct
    void loadPacks() throws IOException {
        Resource[] resources = new PathMatchingResourcePatternResolver()
            .getResources("classpath:compliance/*.json");
        for (Resource r : resources) {
            try (InputStream in = r.getInputStream()) {
                RulePack pack = mapper.readValue(in, RulePack.class);
                packs.put(Platform.valueOf(pack.platform().toUpperCase(Locale.ROOT)), pack);
                log.info("Loaded compliance pack {} v{} ({} rules)",
                    pack.platform(), pack.version(), pack.rules().size());
            }
        }
    }

    /** Run every platform's pack over the brief and return all findings (PASS omitted). */
    public List<Finding> evaluate(CanonicalBrief brief, List<Platform> platforms) {
        List<Finding> findings = new ArrayList<>();
        for (Platform platform : platforms) {
            // implicit capability gate
            if (!adapters.get(platform).capabilities().supports(brief.objective())) {
                findings.add(new Finding(platform, "OBJECTIVE_UNSUPPORTED", Severity.BLOCK,
                    platform + " does not support objective " + brief.objective()));
                continue;
            }
            RulePack pack = packs.get(platform);
            if (pack == null) {
                continue;
            }
            for (RulePack.Rule rule : pack.rules()) {
                evaluateRule(platform, rule, brief).ifPresent(findings::add);
            }
        }
        return findings;
    }

    public boolean hasBlocking(List<Finding> findings) {
        return findings.stream().anyMatch(f -> f.severity() == Severity.BLOCK);
    }

    private java.util.Optional<Finding> evaluateRule(Platform platform, RulePack.Rule rule,
                                                     CanonicalBrief brief) {
        Severity severity = Severity.valueOf(rule.severity().toUpperCase(Locale.ROOT));
        String value = fieldValue(rule.field(), brief);

        if (rule.max() != null && value != null && value.length() > rule.max()) {
            return java.util.Optional.of(new Finding(platform, rule.code(), severity,
                rule.field() + " exceeds " + rule.max() + " chars (" + value.length() + ")"));
        }
        if (rule.deny() != null && value != null) {
            String lower = value.toLowerCase(Locale.ROOT);
            for (String word : rule.deny()) {
                if (lower.contains(word.toLowerCase(Locale.ROOT))) {
                    return java.util.Optional.of(new Finding(platform, rule.code(), severity,
                        "Restricted keyword: \"" + word + "\""));
                }
            }
        }
        if (rule.requireField() != null && rule.whenObjective() != null
                && brief.objective() == Objective.valueOf(rule.whenObjective())) {
            if (isBlank(fieldValue(rule.requireField(), brief))) {
                return java.util.Optional.of(new Finding(platform, rule.code(), severity,
                    "Missing required field for " + brief.objective() + ": " + rule.requireField()));
            }
        }
        return java.util.Optional.empty();
    }

    private String fieldValue(String field, CanonicalBrief brief) {
        if (field == null) {
            return null;
        }
        return switch (field) {
            case "headline" -> brief.headline();
            case "body" -> brief.body();
            case "destination.privacyUrl" -> str(brief.destination().get("privacyUrl"));
            case "destination.value" -> str(brief.destination().get("value"));
            default -> null;
        };
    }

    private static String str(Object o) {
        return o == null ? null : o.toString();
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
