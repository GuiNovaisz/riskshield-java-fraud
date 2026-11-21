# Velocity state and atomicity

Redis is a semantic velocity store, not described as an expendable cache. Single-instance Redis, ZSETs plus atomic Lua registration/read/expiry. Redis Cluster is unsupported: the two entity keys do not share a cluster hash slot. Customer key `rs:v1:c:<SHA256 synthetic customer>`; device key `rs:v1:d:<SHA256 synthetic device>`. Deterministic hashing is pseudonymization only, not encryption.

Customer members are UUIDs. Device members are UUID + ':' + customer hash. Scores are integer event-time milliseconds (exact in Redis IEEE 754 within supported dates). ZADD NX makes retries idempotent. Atomic script does cleanup, insert, expiration and all range reads. Device distinct scan caps at 10001; capacity breach degrades rather than returning truncated safe-looking features. Customer counts use ZCOUNT.

Retention: remove scores older than processing-time minus 70m; late allowance 10m plus longest 1h window. Key idle TTL: 4500 seconds (75m). Late requests do not register Redis state. Empty/inactive keys expire. Redis maxmemory 128 MiB with noeviction: capacity failure becomes REVIEW degradation rather than silent eviction. Memory still depends on traffic within the bounded retention; capacity is not an infinite-throughput guarantee.

PostgreSQL accepted intents provide a conservative per-feature lower bound after Redis restart or an uncertain partial write. Counts are reconciled by max, avoiding false low velocity. One indexed aggregate per evaluation is an explicit correctness/latency trade-off; this is not an all-in-memory latency claim. Concurrent future accepted attempts never enter windows ending before their event time. Pending accepted attempts can increase contemporaneous counts; that is the declared attempt semantics.
