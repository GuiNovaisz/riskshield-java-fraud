# Reproducible synthetic evaluation

Run `scripts/evaluate.ps1 -Seed 20261121`. Main class: `io.riskshield.evaluation.EvaluationMain`.
The runtime and evaluator share `DecisionEngine`, `BehavioralFeatures`, versioned rules and `(t-window,t]` definitions. Offline history contains only previous approved payments; labels are metadata passed exclusively to metrics. Generator `generator-1` uses seeded java.util.Random and stable UUIDs.

Calibration: first 24 simulated hours. Test: subsequent 16 hours. Threshold analysis compares fixed feature snapshots; it is a shadow comparison, not a claim about long-term effects of different approval policies. Costs are configurable JVM properties `demo.fp`, `demo.fn`, `demo.review` (default 10/100/2 arbitrary demo units). Zero-denominator precision/recall are 0 and are explicitly disclosed.

`results/dataset.jsonl` is the versioned regression/evaluation source. A separate small golden fixture is in `golden/`. Neither contains real PII or payment credentials. Fraud prevalence includes all attempts in generated attack bursts, not just attack initiations.
