package io.riskshield.application;

import io.riskshield.domain.*;
import java.time.Instant;
import java.util.UUID;

public record DecisionDocument(UUID decisionId, UUID transactionId, Payment payment,
                               Features features, DecisionEngine.Result result,
                               Ruleset configuration, Instant receivedAt, Instant evaluatedAt) {}
