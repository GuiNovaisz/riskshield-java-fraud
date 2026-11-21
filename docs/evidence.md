# RiskShield v0.1.0 — executed evidence

Historical foundation-review record, captured on 2026-11-21 before feature → dev integration. The no-merge/no-tag/no-release statements below describe that completed mission, not the current publication state. See README for the current release.

Evidence date: 2026-11-21, America/Sao_Paulo. Source verification commit: `7eb381647a3419425f76abe4f1b8ed76c0c610ea`. Later evidence/documentation commits do not change decision-domain behavior. Final branch SHA and final CI run are reported in the delivery report and PR checks, avoiding a self-referential commit hash in this file.

Environment: Windows PowerShell 7, isolated Temurin 21.0.13.1+2, Maven Wrapper 3.9.16, Docker Desktop Linux engine 29.9.2; Testcontainers PostgreSQL 18.1 and Redis 8.4.0. No H2, no browser/GUI, no stress test. Source research and versions: references.md / versions.md. Environment-specific HTTP load includes BCrypt.

Full clean build: **22 tests, zero failures/errors/skips**, including one test executing all 10 frozen golden cases and two independent JVM evaluation processes. Source CI run **44879739067** is **success** (verify, compose, secret-scan, dependency-review). Earlier complete successful run: **44879547326**. Initial dependency-review failure was resolved by enabling dependency graph/vulnerability alerts through the documented repository API; no check was bypassed.

| Proof | Environment / command or test | Expected | Actual | Status |
|---|---|---|---|---|
| R1 Deterministic Decision | Java21, DecisionEngineTest.deterministicExplainableAndBounded | Same feature/config/input => same result | 1000 seeded cases, identical results | PASS |
| R2 Idempotent Intake | PG/Redis IT + scripts/smoke.ps1 | Same ID/payload => original audit | Same decision UUID and document | PASS |
| R3 Duplicate Conflict | FoundationIT.idempotencyConflictAndConcurrentDuplicates | Different payload rejected | DuplicateConflict; API maps 409 | PASS |
| R4 Concurrent Duplicate | Same IT, 16 calls / 8 workers | One decision/effect | One UUID, one DB row, one ZSET member | PASS |
| R5 Rule Reproducibility | zGovernanceMakerCheckerShadowAndVersionHistory | Rule revision preserves old evidence | New NEW_DEVICE v2; old v1 document unchanged | PASS |
| R6 Policy Reproducibility | Same IT, active promotion | Old thresholds/version remain | Original baseline-1 unchanged; new decision candidate version | PASS |
| R7 Score Bounds | 1000 seeded cases + golden | Score in 0..100 | All bounded, combined fixture clamps at 100 | PASS |
| R8 Explainability | Domain/golden + stored DecisionDocument | Contributions reconcile score; full versions/features | Sum/clamp asserted; immutable config snapshot | PASS |
| R9 Velocity Atomicity | redisAtomicWindowsTtlAndOutOfOrder + concurrentUniqueAttemptsKeepAllAuditAndVelocity | No lost updates | 64 direct Redis updates; 32 concurrent unique audits and count 32 | PASS |
| R10 Sliding Window | Redis IT, t / t+60s | Lower bound excluded; longer window retained | 1-minute count=1; 5-minute count=65 | PASS |
| R11 TTL Cleanup | Redis IT | Idle expiration and old member pruning | TTL>4400s; accelerated key expired; >70m old event removed | PASS |
| R12 New Device | Behavioral/domain + FoundationIT.amountDeviceAndLateEventsAreSafe | Novel/known distinguishable | New true, known false | PASS |
| R13 Amount Anomaly | Same + behavioralTimeFilteringAndNoLabelInput | Personal deviation after minimum history | 5x amount detected against prior 1000-cent mean | PASS |
| R14 Event Time | Redis processing-delay regression | Processing delay does not move event window | Same event at processing+300s still count=1 | PASS |
| R15 Out-of-Order | Redis range + behavior reverse-order + late IT | No future contamination or last-state overwrite | Future member excluded; reverse history same features; late has no future approvals | PASS |
| R16 Label Leakage | EvaluationTest.deterministicSeedImbalanceAndLabelIsolation | Future labels never alter features/results | Flipped every label and future timestamp; identical decisions | PASS |
| R17 Feedback Integrity | immutableAuditFeedbackAndLabelLeakage | Correct association and label timing | FK-bound append; early observation rejected | PASS |
| R18 Decision Immutability | Same IT + PG triggers | Historical decision cannot update | UPDATE rejected; label does not rewrite document | PASS |
| R19 Shadow Isolation | Governance IT | Candidate leaves active unchanged | baseline-1 APPROVE retained while candidate REVIEW | PASS |
| R20 Shadow Comparison | Same IT | Difference stored | shadow_comparisons.disagreed=true | PASS |
| R21 Redis Failure | Test spy + scripts/failure-proofs.ps1 real stop | Degraded at least REVIEW | REVIEW, VELOCITY_STORE; liveness/readiness UP | PASS |
| R22 DB Failure | DB write-failure trigger + real PG stop | No success without audit | Exception/rollback; live HTTP503, liveness UP; retry succeeds without Redis double count | PASS |
| R23 Rule Authorization | SecurityTest, actual SecurityFilterChain | ENGINE cannot manage rules | 403; unauthenticated401; other wrong roles403 | PASS |
| R24 Maker/Checker | Governance IT | Maker cannot approve own version | Self-approval rejected; distinct checker approves/shadows/activates | PASS |
| R25 Evaluation Reproducibility | independentJvmReportsAreByteReproducible + CI diff | Same seed/config/metadata => same dataset/report bytes | Independent Java processes byte-identical | PASS |
| R26 Imbalance-Aware Metrics | evaluation/results/evaluation.json | Confusion matrix, precision/recall/F1/FPR/FNR | Test TP63 FP55 TN3104 FN55; all requested metrics | PASS |
| R27 Threshold Trade-Off | EvaluationMain + validation report | Multiple cost-aware pairs | 12 pairs; selected30/65, final validation agrees | PASS |
| R28 Baseline | Shared production engine in chronological Java replay | Measured rules/features baseline | Test precision/recall/F1=0.533898; no accuracy-only claim | PASS |
| R29 ML Gate | docs/ml-assessment.md | Exactly one honest decision | ML_NOT_INCLUDED_IN_V0.1.0; no measured candidate gain | PASS |
| R30 Load Baseline | k6 five 15s scenarios, docs/artifacts/load-summary.json | Throughput and p50/p95/p99/errors measured | Steady21.55req/s;74.56/117.34/173.03ms;0 errors | PASS |
| R31 Load Correctness | k6 checks + PG/Redis post-load counts | No lost velocity/duplicate audit/missing audit |1539 unique load decisions;0 missing/invalid;duplicate1;hot PG459=Redis459 | PASS |
| R32 Observability | Prometheus API, Tempo trace, Grafana provisioning | Trace of decision stages available | decision-trace.json has validation/features/velocity/rules/scoring/persistence/total; scrape UP | PASS |
| R33 Metric Cardinality | risk-metrics.prom + normalized label source | No customer/device/transaction/account/decision/IP labels | Fixed stage/outcome/reason vocabulary; custom reasons normalize OTHER | PASS |
| R34 Secret Scan | Gitleaks8.30.1 full Git history + tracked archive; CI | No secrets in Git/history/artifacts | Zero findings in history/tracked artifacts; local ignored .env excluded from delivery | PASS |
| R35 Docker Clean Start | CI compose job on fresh hosted runner | Build and stack smoke from new volumes | CI44879739067 compose success, authenticated duplicate smoke and load checks | PASS |
| R36 Clean Rebuild | CI verify checkout + local clean verify | Java21 clean compile/test | 22 tests pass; no preexisting build state required | PASS |

## Measured artifacts

- `artifacts/tests-summary.json`: sanitized suite/case counts.
- `artifacts/load-summary.json`, `load-correctness.json`: actual HTTP measurements and durable distribution.
- `artifacts/decision-trace.json`, `risk-metrics.prom`: real trace/stage metrics without request payloads or credentials.
- `artifacts/failure-proofs.json`: real Redis/PostgreSQL outage response contracts.
- `artifacts/dependency-review.json`: repository dependency diff, 23 added dependencies, no reported vulnerabilities in that diff. This is not a claim of complete perpetual vulnerability absence.
- `../evaluation/results/evaluation.json` and `.md`: measured held-out metrics, cost grid, seed, split, source commit and dataset checksum.

## Findings audit

Critical: **0 unresolved**. Important: **0 unresolved**. Optional: production TLS/OAuth2 resource server, dedicated DB runtime role, durable telemetry storage, archival tooling, longer soak/capacity analysis, real representative dataset, ML only after a measured benefit, and future Risk Operations Console (not started).

Resolved findings: real Basic auth lacked `{bcrypt}` prefix (fixed + actual-password test); concurrent duplicate inserts could race on redundant composite unique index (Flyway V2 + exact concurrency regression); cross-JVM JSON map ordering differed (ordered confusion matrix + subprocess byte-comparison); Tempo 192MiB OOM on compaction (512MiB/Go384MiB + verified recovery); port18080 belonged to another user project (moved to28080, existing project preserved).

Honest limits: synthetic effectiveness only; main dataset produced no BLOCK outcomes; block cut is weakly calibrated; standalone Redis; arrival-order knowledge affects available behavior; no real location/travel inference; no account ownership integration; trusted DB owner can disable append-only controls; local Basic/loopback deployment only; audit archival not implemented; bounded 15s loads do not prove capacity or long-term stability. These are declared scope boundaries, not hidden production claims.

No merge into dev/main, no main update beyond initial repository bootstrap, no tag, no release, no frontend and no v0.2.0 implementation.
