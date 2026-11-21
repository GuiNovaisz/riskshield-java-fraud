# ADR 002-redis: Redis velocity

Date: 2026-11-21

Status: Accept

Decision: Atomic Lua with ZSET NX and TTL; durable intent count reconciliation. Standalone only, not Redis Cluster.

Consequences: See architecture.md, failure-policy.md and evidence.md for implementation trade-offs and executed proofs.
