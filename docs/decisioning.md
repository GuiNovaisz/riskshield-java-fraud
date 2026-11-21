# Decision path and contracts

POST `/risk/evaluations` requires ENGINE; returns `DecisionDocument` with decisionId, transactionId, payment, features, result, complete configuration, receivedAt/evaluatedAt. `result` contains decision, riskScore, rawScore, contributions, ruleVersion, policyVersion and degraded. No input label field exists. Unknown properties are rejected.

Same parsed canonical payment yields the same SHA-256 fingerprint regardless of JSON property order. Same transactionId + same fingerprint returns the stored document even after configuration changes. Different fingerprint returns 409. Identity is global, not per caller. Millisecond precision and stable typed serialization avoid timestamp/decimal fingerprint ambiguity.

The committed intent defines an accepted attempt. Pending attempts still contribute to durable velocity; this is conservative during failure/retry. Behavioral history uses only prior APPROVE decisions already available to the evaluator. There is no silent queue or success before audit commit. Retries require the same transaction identity.

Errors use RFC 9457 Problem Details with 400,401,403,404,409,413,429,503; internal SQL/configuration/token details are suppressed. Review queries are bounded pages of 1..100, page 0..10000.
