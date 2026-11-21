package io.riskshield.domain;

import java.time.Instant;
import java.util.*;

public record Ruleset(String version, Instant effectiveFrom, Instant effectiveUntil,
                      int reviewThreshold, int blockThreshold, List<Rule> rules) {
    public Ruleset {
        if (version == null || !version.matches("[a-zA-Z0-9._-]{1,48}")) throw new IllegalArgumentException("invalid version");
        if (effectiveFrom == null || effectiveUntil == null || !effectiveFrom.isBefore(effectiveUntil)) throw new IllegalArgumentException("invalid effective interval");
        if (reviewThreshold < 1 || reviewThreshold >= blockThreshold || blockThreshold > 100) throw new IllegalArgumentException("invalid thresholds");
        if (rules == null || rules.isEmpty() || rules.size() > 32) throw new IllegalArgumentException("1..32 rules required");
        rules = rules.stream().sorted(Comparator.comparingInt(Rule::priority).thenComparing(Rule::id)).toList();
        if (rules.stream().map(Rule::id).distinct().count() != rules.size()) throw new IllegalArgumentException("duplicate rule id");
    }
    public boolean effective(Instant at) { return !at.isBefore(effectiveFrom) && at.isBefore(effectiveUntil); }
    public record Rule(String id, String version, String description, boolean enabled, int priority,
                       Features.Signal signal, long threshold, int contribution,
                       Instant effectiveFrom, Instant effectiveUntil) {
        public Rule {
            if (id == null || !id.matches("[A-Z][A-Z0-9_]{0,47}") || version == null || !version.matches("[a-zA-Z0-9._-]{1,48}")) throw new IllegalArgumentException("invalid rule identity");
            if (description == null || description.isBlank() || description.length() > 240 || signal == null || threshold < 1 || contribution < -100 || contribution > 100 || priority < 0 || priority > 1000)
                throw new IllegalArgumentException("invalid rule");
            if (effectiveFrom == null || effectiveUntil == null || !effectiveFrom.isBefore(effectiveUntil)) throw new IllegalArgumentException("invalid rule interval");
        }
        public boolean fires(Features f, Instant at) { return enabled && !at.isBefore(effectiveFrom) && at.isBefore(effectiveUntil) && f.value(signal) >= threshold; }
    }
    public static Ruleset baseline() {
        Instant from = Instant.parse("2020-01-01T00:00:00Z"), until = Instant.parse("2100-01-01T00:00:00Z");
        return new Ruleset("baseline-1", from, until, 30, 65, List.of(
            new Rule("NEW_DEVICE", "1", "Device absent from prior 30-day history", true, 10, Features.Signal.NEW_DEVICE, 1, 18, from, until),
            new Rule("NEW_MERCHANT", "1", "Merchant absent from prior 30-day history", true, 20, Features.Signal.NEW_MERCHANT, 1, 8, from, until),
            new Rule("NEW_COUNTRY", "1", "Country absent from prior 30-day history", true, 30, Features.Signal.NEW_COUNTRY, 1, 12, from, until),
            new Rule("AMOUNT_SPIKE", "1", "At least 5 prior events and amount exceeds 3x personal mean", true, 40, Features.Signal.AMOUNT_SPIKE, 1, 32, from, until),
            new Rule("VELOCITY_SPIKE", "1", "At least 6 accepted attempts in (t-1m,t] including current", true, 50, Features.Signal.CUSTOMER_1M, 6, 35, from, until),
            new Rule("SUSTAINED_VELOCITY", "1", "At least 12 accepted attempts in (t-5m,t]", true, 60, Features.Signal.CUSTOMER_5M, 12, 20, from, until),
            new Rule("DEVICE_SHARING", "1", "At least 4 customers using device in (t-1h,t]", true, 70, Features.Signal.DEVICE_SHARING, 4, 35, from, until)));
    }
}
