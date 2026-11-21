# Local operations

Prerequisites: Docker Desktop Linux engine, PowerShell 7, Java 21 for host verification (container build includes Java), Git. Run `scripts/dev-up.ps1 -Build`; credentials are generated in ignored `.env`. `scripts/smoke.ps1` validates readiness and duplicate behavior. API `http://localhost:28080`, Grafana `http://localhost:13000`, Prometheus `http://localhost:19090`, Tempo `http://localhost:13200`. Passwords are read locally from `.env`, never pasted into reports.

`scripts/dev-down.ps1` stops only project riskshield and preserves volumes. No commands stop unrelated user processes or containers. A clean-room check uses a distinct Compose project and new volumes; never delete volumes belonging to another task. Backup PostgreSQL before intentional retention/archival changes. Redis can rebuild safe count floors from durable intents; it is still part of the semantic feature path.

Readiness must be UP before intake. Retry 503/ambiguous responses with the same transaction UUID. 409 means identity conflict or governance transition rejection; do not blindly retry a changed payload. 429 supplies Retry-After. Redis outage yields degraded REVIEW; DB outage yields no successful durable response. Graceful shutdown drains in-flight work up to 20s, container allows 30s.

Intake/decision/feedback/governance remain durable and append-only in v0.1.0. No automated audit deletion or archival implemented. Feature lookbacks are bounded and Redis keys expire; PostgreSQL audit storage grows and requires capacity planning. Demo resource limits are not production sizing.
