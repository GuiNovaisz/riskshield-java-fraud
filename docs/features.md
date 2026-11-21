# Feature pipeline

```mermaid
flowchart LR
  Intent[Accepted intent] --> Lua[Atomic Redis register + ranges]
  Intent --> PG[Indexed durable velocity aggregate]
  Lua --> Reconcile[Per-feature maximum]
  PG --> Reconcile
  History[Prior approved payments before event time] --> Behavior[30-day mean and novelty]
  Reconcile --> Snapshot[features-1 immutable snapshot]
  Behavior --> Snapshot
  Snapshot --> Rules
```

Counts include the current intent, windows are `(t-window,t]`. Features: customer counts 1m/5m/1h, device count 5m, distinct customers/device 1h; prior approved count, new device/merchant/country, personal amount spike, rounded historical mean cents. Exact BigDecimal cross multiplication tests >3x mean without monetary floating point. The rounded mean is explanatory, not the comparator input.

Lineage is `features-1` + bounded approved event history + event-time policy + immutable config. Evaluation and online processing call the same Java behavior implementation and engine. Offline velocity independently implements the documented range definition; boundary integration tests guard drift.
