# Threat model

Trust boundaries: synthetic producer → authenticated intake; analyst → labels; maker/checker → config; application → PostgreSQL/Redis; telemetry network → collector/Prometheus. Assets: immutable decisions, accepted-attempt identity, rule/policy versions, trustworthy feature snapshots and credentials. Local Compose is an isolated demonstration, not production security certification.

| Threat | Control | Residual limitation |
|---|---|---|
| Rule/policy tampering | ADMIN-only immutable bundle, distinct maker/checker, version constraints, append-only governance | DB owner and host admin remain trusted |
| Replay / duplicate manipulation | UUID primary key, canonical payload hash, row locks, 409 conflicting payload | Caller can mint new identities; velocity still counts attempts |
| Privilege escalation / object authorization | Deny-all default; ENGINE intake, ANALYST feedback/review, AUDITOR audit, ADMIN config | Roles are service-level, no multi-tenant object ownership |
| Decision manipulation | Pure bounded engine, validated enums/config, snapshot and audit triggers | Upstream synthetic identifiers are trusted assertions |
| Data poisoning | Baseline only earlier approved events, bounded histories, no label features | Approved undetected fraud may still contaminate mean/device history |
| Label poisoning | ANALYST-only, compatible outcomes, actor from Principal, append-only feedback | Analyst may be wrong; no full adjudication platform |
| Feature store outage / capacity | REVIEW degradation, durable count floor, noeviction, bounded timeouts | PostgreSQL fallback adds load; no HA claim |
| SQL injection | JDBC bound values, no dynamic expression execution | SQL/migration privileges need separate deployment roles outside demo |
| Denial of service | 32 KiB body cap including chunked requests, bounded IDs/pages/history, 24 concurrent admissions, DB/Redis timeouts, container limits | No distributed rate-limit gateway; trusted localhost demo only |
| Sensitive-data leakage | Synthetic-only schema, hashed Redis keys, no request/token logging, scrubbed Problem Details | Synthetic actor/reason audit may contain operator-entered text; avoid secrets |
| Supply chain | BOM, checked wrapper hash, image digests, SHA-pinned Actions, dependency review, history secret scan | Digest pinning needs monitored upgrades; not perpetual security assurance |
| Exceptional conditions | 503 durable failure contract; no partial config activation; safe Redis degradation | Network after commit can hide response; retry required |

OWASP Top 10 2025 and API Top 10 2023 were reviewed (references.md). There is no ML artifact to tamper with. Adversarial limits: slow fraud, device/identity rotation, distributed low-volume fraud and threshold probing can evade these rules. `ruleEvasionAndCombinedSignals` demonstrates a single novelty signal can be evaded; combining personal amount/velocity raises risk. This does not claim comprehensive adversarial detection.
