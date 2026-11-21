# ADR 003-kafka: Kafka

Date: 2026-11-21

Status: Reject for v0.1.0

Decision: No asynchronous operational requirement justifies broker; synchronous event intake and offline replay suffice.

Consequences: See architecture.md, failure-policy.md and evidence.md for implementation trade-offs and executed proofs.
