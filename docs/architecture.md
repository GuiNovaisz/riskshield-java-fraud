# Architecture

Modular monolith: pure Java `domain`; transactional `application`; Spring/JDBC/Redis/HTTP `adapter`; offline Java `evaluation`. Bounded contexts are decisioning, feature engineering, configuration governance, audit/feedback and evaluation. No frontend, Kafka, ML service or cloud substrate.

```mermaid
flowchart LR
  Producer[Synthetic payment producer] --> API[RiskShield monolith]
  Analyst[Analyst] --> API
  Admin[Maker / Checker] --> API
  API --> PG[(PostgreSQL durable intents and audit)]
  API --> Redis[(Redis velocity state)]
  API --> OTel[OTel Collector]
  OTel --> Tempo[Tempo]
  Prom[Prometheus] --> API
  Grafana[Grafana] --> Prom
  Grafana --> Tempo
```

Only synchronous durable audit is in the response contract. No distributed exactly-once claim: accepted intents may outlive failed evaluations, Redis registration is idempotent, retries are safe, DB reconciliation prevents undercounts after Redis restart. Per-customer/device database advisory locks coordinate multiple instances. Hash collisions can serialize unrelated entities but cannot weaken invariants. Different entities remain concurrent.

```mermaid
flowchart TD
  Request --> Validation --> DurableIntent[Commit accepted intent]
  DurableIntent --> Lock[Lock transaction and entity keys]
  Lock --> Existing{Already decided?}
  Existing -->|yes| Stored[Return original snapshot]
  Existing -->|no| Features[Features and velocity reconciliation]
  Features --> Rules --> Score --> Policy --> Audit[Immutable decision insert]
  Audit --> Shadow[Optional candidate on same features]
  Shadow --> Commit[Commit audit and comparison]
  Commit --> Response
```

Database failure before commit returns 503, never fictitious approval. HTTP ambiguity after commit is resolved by retrying the same identity. Lock/statement timeouts bound waiting. Runtime retains immutable audit; a separate operational archival policy is required before long-running use.
