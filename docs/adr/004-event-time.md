# ADR 004-event-time: Event-time strategy

Date: 2026-11-21

Status: Accept

Decision: Feature windows use UTC occurrence time; policy uses evaluation time. >10m late degrades; >30d rejected. No mutable last-device profile.

Consequences: See architecture.md, failure-policy.md and evidence.md for implementation trade-offs and executed proofs.
