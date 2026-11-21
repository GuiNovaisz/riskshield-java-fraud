# Dependency failure policy

| Failure | Impact | Decision contract | Reason |
|---|---|---|---|
| Redis timeout/capacity | Online temporal state missing | Durable velocity reconciliation; mark VELOCITY_STORE; at least REVIEW | Never turn infrastructure failure into score=0 approval |
| Redis restart/state loss | Incomplete online counters | Per-feature max with durable accepted-intent counts | Avoid silent undercount |
| PostgreSQL write/read/commit failure | Cannot guarantee durable decision | 503, retry same identity | Never report unpersisted approval |
| DB failure after Redis register | Redis contains durable accepted attempt, audit pending | No success; retry uses NX and immutable intake | No lost increment or double count |
| Expired/invalid active configuration | Cannot apply complete ruleset | 503; no partial activation | Safe configuration loading |
| OTel/Prometheus outage | Telemetry unavailable | Decisions unaffected, bounded asynchronous exporter | Observability is not audit |
| Behavioral capacity >5000 | Truncated baseline | BEHAVIOR_CAPACITY; at least REVIEW | Missing history disclosed |
| Event older than 10m | Retained online window not reliable | LATE_EVENT; at least REVIEW | Explicit late-event policy |
| Admission saturation | Resource exhaustion risk | 429 + Retry-After | Bounded local concurrency |

Liveness excludes optional Redis. Readiness includes PostgreSQL and application readiness, excludes Redis because controlled degradation is supported. No failure injection endpoint exists in runtime; deterministic failures are test-only spies and DB triggers.
