# Version selection

Research date: 2026-11-21. Explicitly verified Boot 4.2.0 as stable rather than accepting the mission's assumption. Java 21 is within its documented supported Java range. Boot BOM owns compatible versions; no independent Spring Security/Redis/observability overrides.

| Component | Choice | Evidence / compatibility |
|---|---|---|
| Java | Eclipse Temurin 21.0.13.1+2, release 21 | Adoptium v3 API, SHA-256 verified isolated archive |
| Spring Boot | 4.2.0 | Official requirements + dependency coordinates |
| Spring Security | BOM 7.2.0 | Same Boot BOM |
| Spring Framework | BOM 7.1.0 | Boot requirements |
| Lettuce | BOM 7.8.0.RELEASE | Boot Redis starter |
| Micrometer | BOM 1.18.0 | Boot actuator + timers |
| OpenTelemetry | BOM 1.68.0 | Micrometer OTel bridge + OTLP exporter |
| Flyway | BOM 12.11.0 | PostgreSQL module included |
| Testcontainers | BOM 2.0.5 | 2.x artifact names, JUnit integration |
| Maven | 3.9.16 | Supported stable line; wrapper |
| PostgreSQL | 18 series | Flyway verified 18; 19 too new for documented verification |
| Redis | 8 series | Atomic ZSET/Lua APIs; standalone only |
| Kafka / ML | omitted | ADRs |

Actual Maven dependency tree, container digests and runtime versions are evidence artifacts generated during verification. Declared compatibility is distinct from executed integration tests.
