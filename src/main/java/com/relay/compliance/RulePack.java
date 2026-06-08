package com.relay.compliance;

import java.util.List;

/** Versioned per-platform policy pack loaded from classpath JSON (compliance/{platform}.json). */
public record RulePack(String platform, String version, List<Rule> rules) {

    /**
     * A single declarative rule. Only the fields relevant to its kind are set:
     * <ul>
     *   <li>{@code max} + {@code field} → length check on a text field</li>
     *   <li>{@code deny} + {@code field} → restricted keyword check</li>
     *   <li>{@code requireField} + {@code whenObjective} → conditional required field</li>
     * </ul>
     */
    public record Rule(
        String code,
        String field,
        Integer max,
        List<String> deny,
        String requireField,
        String whenObjective,
        String severity) {}
}
