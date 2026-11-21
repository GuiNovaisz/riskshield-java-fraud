# Measured local performance

Windows host → Docker Desktop Linux, Java 21 container, application capped at 2 CPUs/768 MiB, PostgreSQL 384 MiB, Redis 192 MiB, Prometheus/Grafana 192 MiB each, collector 128 MiB. Tempo was initially capped at 192 MiB and OOM-killed during trace compaction after the short runs; corrected to 512 MiB with Go soft limit 384 MiB. No impact on durable decisions. This is why telemetry availability is verified independently.

`docs/artifacts/load-summary.json` contains measured HTTP latency, including BCrypt authentication, network and serialization, not just engine computation. Each scenario is 15 seconds, 4 VUs except burst 8 and concurrency 12. This is a bounded demonstration, not saturation or stress capacity testing.

| Scenario | Requests | Req/s | p50 ms | p95 ms | p99 ms | Errors |
|---|---:|---:|---:|---:|---:|---:|
| steady | 327 | 21.55 | 74.56 | 117.34 | 173.03 | 0 |
| burst | 361 | 23.86 | 298.96 | 391.48 | 400.61 | 0 |
| concurrency | 391 | 25.57 | 479.49 | 499.89 | 589.60 | 0 |
| duplicate | 466 | 30.76 | 111.46 | 175.35 | 181.35 | 0 |
| velocity | 459 | 30.38 | 109.24 | 170.42 | 180.43 | 0 |

All checks passed, zero degraded decisions in the healthy measured runs. Post-load DB: 1539 unique load decisions / 1539 identities, zero missing audits, zero invalid scores, one logical duplicate-storm decision. Hot-customer accepted intents=459 and Redis ZCARD=459. Strong concurrency also covered by exact-count integration tests. The steady run preceded the last documentation/evaluation-only changes; no runtime velocity/scoring change after these measurements.

Demo target derived after measurement: p95 HTTP <750ms at up to 12 VUs in this environment, zero HTTP/correctness errors for these bounded scenarios. This target is a local regression budget, not a production SLO or throughput capacity claim. Authentication dominates much of the measured HTTP time; no unproven optimization or claim of sub-10ms production decisions.

Scripts support reruns. k6 checks enforce correctness, not a brittle latency assertion across different CI hosts. Raw summaries remain in ignored work/load; sanitized versioned summary is portable. Longer soak, HA/cluster tests and production sizing remain out of scope.
