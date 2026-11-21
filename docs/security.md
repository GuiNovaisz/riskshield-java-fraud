# Security

Stateless Spring Security Basic is deliberately restricted to localhost demonstration. Each service identity has a distinct randomly generated 256-bit local password; only BCrypt hashes reside in the application user store. `.env` is ignored and excluded from Docker context/artifacts. No authorization server or custom JWT issuer. Non-local deployment requires TLS and an externally managed OAuth2 resource server, secret delivery and network isolation.

ENGINE can evaluate; ANALYST can review/read decisions and append feedback; AUDITOR reads decisions/governance/shadow evidence; ADMIN creates/configures bundles with independent maker/checker. Deny-all default. No cross-role implicit privileges. The bootstrap baseline is source-reviewed, not a fabricated second human approval. Config draft/activation logs actor and before/after state plus immutable version.

CSRF is disabled for the stateless API; there is no browser UI or session cookie. Basic credentials can be cached by browsers, so this configuration must not be exposed as a browser-facing application. CORS is not enabled. Actuator exposes only health and Prometheus; scrape endpoints contain no synthetic entity IDs. All externally mapped ports bind loopback; DB/Redis/OTLP ports are internal.

Application container: UID 10001, read-only filesystem, tmpfs /tmp, no capabilities, no-new-privileges and limited memory/CPU. PostgreSQL Compose initialization uses its local database owner for migration/runtime; dedicated least-privilege deployment roles are a documented production gap. Triggers prevent ordinary SQL update/delete but are not tamperproof against the database owner disabling triggers.

Secret scanning covers full Git history. Dependency review blocks high/critical introduced vulnerabilities; pinned artifacts must be upgraded deliberately. Security tests verify unauthenticated and wrong-role denials and invalid/unknown properties. No real card/CPF/customer/IP data is collected.
