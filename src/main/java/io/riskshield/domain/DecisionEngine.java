package io.riskshield.domain;

import java.time.Instant;
import java.util.List;

/** Pure deterministic engine. No label, infrastructure or system clock dependency. */
public final class DecisionEngine {
    public Result evaluate(Features features, Ruleset config, Instant evaluatedAt) {
        if (!config.effective(evaluatedAt)) throw new IllegalStateException("ruleset outside effective interval");
        List<Contribution> reasons = config.rules().stream().filter(r -> r.fires(features, evaluatedAt))
            .map(r -> new Contribution(r.id(), r.version(), r.contribution(), features.value(r.signal()), r.threshold())).toList();
        int raw = reasons.stream().mapToInt(Contribution::points).sum();
        int score = Math.clamp(raw, 0, 100);
        Outcome outcome = score >= config.blockThreshold() ? Outcome.BLOCK : score >= config.reviewThreshold() ? Outcome.REVIEW : Outcome.APPROVE;
        // Missing evidence can never create an APPROVE. Preserve BLOCK when supported by surviving signals.
        if (!features.degradedComponents().isEmpty() && outcome == Outcome.APPROVE) outcome = Outcome.REVIEW;
        return new Result(outcome, score, raw, reasons, config.version(), config.version(), !features.degradedComponents().isEmpty());
    }
    public enum Outcome { APPROVE, REVIEW, BLOCK }
    public record Contribution(String reasonCode, String ruleVersion, int points, long observed, long threshold) {}
    public record Result(Outcome decision, int riskScore, int rawScore, List<Contribution> contributions,
                         String ruleVersion, String policyVersion, boolean degraded) {}
}
