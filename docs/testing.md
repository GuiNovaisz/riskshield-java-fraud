# Verification

`scripts/verify.ps1`: clean Maven build, unit/MVC/evaluation/golden tests, then Testcontainers integration tests with actual PostgreSQL 18.1 and Redis 8.4.0. No H2. Deterministic seeded loops cover 1000 score/determinism cases. Integration concurrency uses bounded executor pools, future joins and exact counts, not randomized sleeps. TTL polling is bounded at three seconds.

Proofs include duplicate/conflict/concurrent duplicates, 64 atomic Redis updates, window lower-bound exclusion, out-of-order exclusion of future events, key expiration, 32 concurrent unique durable decisions, personal amount/novel device, late events, immutable audit, delayed feedback, independent maker/checker, shadow comparison/isolation, configuration activation/history, Redis failure and DB write failure plus retry idempotence. Failure injection exists only inside tests. Golden fixtures are frozen after calibration and intentionally distinct from main evaluation.

`scripts/load-test.ps1` supports steady, burst, concurrency, duplicate, velocity, limited 15s runs. It checks HTTP outcomes/score bounds/audit identities and duplicate equivalence. Strong exact DB/Redis post-load invariants are verified separately, not inferred from HTTP 200 alone. Reports are environment-specific. CI also clean-builds the Docker stack, smoke-tests and runs three representative load scenarios.

`docs/evidence.md` is the authority for executed status. A declared test does not prove it was run; FAIL/BLOCKED must remain explicit until rerun succeeds. CI run IDs and commit SHAs must refer to actual executions.
