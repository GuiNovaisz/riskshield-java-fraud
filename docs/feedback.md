# Feedback and label lifecycle

```mermaid
sequenceDiagram
  participant E as Engine
  participant D as Immutable decision
  participant A as Analyst
  participant F as Append-only feedback
  E->>D: decision at T0
  A->>F: label observed at T+n
  F->>D: foreign-key association only
  Note over D,F: Original decision never rewritten
```

POST `/risk/feedback`: feedback UUID, decision UUID, label, manual outcome, observedAt, bounded reason. FRAUD ↔ CONFIRMED_FRAUD; LEGITIMATE ↔ LEGITIMATE; UNKNOWN ↔ INCONCLUSIVE. Observation must be no earlier than decision and no later than injected clock. Repeated feedback records are separate audit events; identical feedback UUID conflicts. Actor is authenticated, never assigned from payload.

Feedback table is append-only. Corrections append a new event; consumers must select latest recorded assessment deliberately. The decision domain cannot receive labels and queries do not join feedback into features. Evaluation tests flip every label and future label timestamp while asserting identical features/results.
