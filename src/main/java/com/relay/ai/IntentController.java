package com.relay.ai;

import com.relay.shared.Objective;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI intent: natural-language goal → suggested objective, destination, starter targeting and
 * copy. Stateless. The spine ships a transparent heuristic behind the same contract a real LLM
 * gateway will implement in phase 3, so the wizard works end-to-end today.
 */
@RestController
@RequestMapping("/v1/ai/intent")
public class IntentController {

    @PostMapping
    public IntentResponse infer(@RequestBody IntentRequest req) {
        String goal = req.goal().toLowerCase(Locale.ROOT);

        Objective objective;
        Map<String, Object> destination;
        if (contains(goal, "call", "phone", "ring")) {
            objective = Objective.CALLS;
            destination = Map.of("type", "phone", "value", "");
        } else if (contains(goal, "lead", "sign up", "signup", "quote", "enquir", "inquir")) {
            objective = Objective.LEADS;
            destination = Map.of("type", "leadform", "privacyUrl", "");
        } else if (contains(goal, "buy", "sale", "shop", "purchase", "order", "checkout")) {
            objective = Objective.SALES;
            destination = Map.of("type", "website", "value", "");
        } else if (contains(goal, "install", "download", "app")) {
            objective = Objective.APP_PROMOTION;
            destination = Map.of("type", "app", "value", "");
        } else if (contains(goal, "visit", "traffic", "click")) {
            objective = Objective.TRAFFIC;
            destination = Map.of("type", "website", "value", "");
        } else {
            objective = Objective.AWARENESS;
            destination = Map.of("type", "website", "value", "");
        }

        String geo = extractGeo(goal);
        String subject = subject(goal);
        return new IntentResponse(
            objective,
            destination,
            geo == null ? List.of() : List.of(geo),
            "Need " + subject + "? We can help today",
            "Fast, reliable " + subject + (geo == null ? "" : " in " + geo) + ". Get started now.");
    }

    private static String subject(String goal) {
        for (String s : List.of("plumber", "plumbing", "electrician", "dentist", "lawyer",
                "cleaner", "roofer", "contractor")) {
            if (goal.contains(s)) {
                return s + "s";
            }
        }
        return "local services";
    }

    private static String extractGeo(String goal) {
        for (String city : List.of("bengaluru", "bangalore", "mumbai", "delhi", "london",
                "new york", "san francisco", "austin")) {
            if (goal.contains(city)) {
                return Character.toUpperCase(city.charAt(0)) + city.substring(1);
            }
        }
        return null;
    }

    private static boolean contains(String haystack, String... needles) {
        for (String n : needles) {
            if (haystack.contains(n)) {
                return true;
            }
        }
        return false;
    }

    public record IntentRequest(@NotBlank String goal) {}

    public record IntentResponse(
        Objective objective,
        Map<String, Object> destination,
        List<String> geo,
        String headline,
        String body) {}
}
