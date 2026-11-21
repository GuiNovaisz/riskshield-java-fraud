# Official research — 2026-11-21

Read before implementation; sources are primary documentation. Rolling documentation may change after this date.

| Technology | Official reference | Design consequence |
|---|---|---|
| Java 21 | https://docs.oracle.com/en/java/javase/21/ | LTS target, integral cents, injected Clock |
| Spring Boot | https://docs.spring.io/spring-boot/system-requirements.html | 4.2.0 stable, Java 17–27 supported; Java 21 selected |
| BOM | https://docs.spring.io/spring-boot/appendix/dependency-versions/coordinates.html | Managed compatible Spring Security, Redis client, observability, Flyway, Testcontainers |
| Spring Security | https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html | Resource server considered; local stateless Basic chosen, no authorization server |
| PostgreSQL | https://www.postgresql.org/docs/current/explicit-locking.html | Unique constraints, transaction locks, append-only triggers |
| Redis | https://redis.io/docs/latest/develop/programmability/eval-intro/ | Lua atomic operations, NX deduplication |
| Redis ranges | https://redis.io/docs/latest/commands/zrangebyscore/ | Event-time windows with exclusive lower bound |
| Kafka | https://kafka.apache.org/documentation/ | Considered and omitted: synchronous durable decisions do not justify broker |
| OpenTelemetry | https://opentelemetry.io/docs/languages/java/ | Traces via Micrometer bridge and OTLP |
| Boot tracing | https://docs.spring.io/spring-boot/reference/actuator/tracing.html | Auto-configured tracing supported |
| Micrometer | https://docs.micrometer.io/micrometer/reference/concepts/timers.html | Histograms, fixed bounded stage tags |
| Testcontainers | https://java.testcontainers.org/quickstart/junit_5_quickstart/ | Real PostgreSQL/Redis integration proofs |
| Flyway | https://documentation.red-gate.com/flyway/reference/database-driver-reference/postgresql-database | PostgreSQL module required; choose verified PostgreSQL 18 over newly released 19 |
| OWASP | https://owasp.org/Top10/2025/ | Authorization, exceptional condition handling, injection, logging |
| OWASP API | https://owasp.org/API-Security/editions/2023/en/0x11-t10/ | Role separation, bounded resource use, strict request schemas |
| Maven | https://maven.apache.org/docs/history.html | Supported 3.9.16 line; avoid Maven 4 RC |
| Docker | https://docs.docker.com/build/building/best-practices/ | Multi-stage, non-root, pinned images |
| Spring Redis | https://docs.spring.io/spring-boot/reference/data/nosql.html | Lettuce + StringRedisTemplate |

No ML library selected. ML must demonstrate value over the measured baseline before inclusion.
