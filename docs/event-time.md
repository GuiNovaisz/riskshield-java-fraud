# Event time versus processing time

`occurredAt` is UTC event time; `receivedAt` is first durable intake time; `evaluatedAt` uses injected Clock, UTC processing time. Millisecond precision required. Future >30s rejected; past >30 days rejected; lateness >10m marks LATE_EVENT, does not register velocity, evaluates available bounded behavior and forces at least REVIEW. Replayed identical requests still return the original logical decision.

Out-of-order accepted events within 10m use their own historical windows, exclude future-event records and never overwrite mutable last-device/location state. Same-timestamp behavioral events are excluded (strictly earlier timestamps) to avoid arbitrary same-time ordering. Velocity includes all accepted attempts through the current timestamp, including pending attempts. Online decisions reflect knowledge available on arrival; offline chronological replay may differ after a reordering because knowledge differs. This is explicit, not a promise of arrival-order-invariant decisions.

Historical audits are immutable. Replay is a separate evaluation run, never an update to an original decision. Rule/policy effective selection is processing-time based; offline simulator supplies its simulated decision clock explicitly.
