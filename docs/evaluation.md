# Evaluation, replay and false positives

Execute `scripts/evaluate.ps1 -Seed 20261121`. Java generator → chronological replay using production engine/features → confusion matrix → threshold grid → JSON/Markdown. `evaluation/results` contains actual output, not illustrative results. Regression tests flip labels without changing decisions and compare seeded datasets. The report includes seed, generator/dataset/rule/policy/feature/model versions, application commit, logical report timestamp, distribution and dataset SHA-256.

The first 24 simulated hours calibrate thresholds; the next 16 are held out. No ML training occurs. Fraud label observation time is seven days after event and is never a feature. Positive prediction is REVIEW or BLOCK, not confirmed fraud. Precision/recall/F1/FPR/FNR and confusion matrix use this definition; approval/review/block rates and affected monetary cents are reported separately. PR-AUC/ROC-AUC are not claimed or computed.

False positives are central: every legitimate flagged transaction contributes to demo FP cost and its cents are included in legitimate reviewed/blocked amount. Costs 10/100/2 are arbitrary scenario units, not bank economics. Threshold comparisons use fixed scored snapshots (shadow), not different history-selection trajectories. Baseline result reflects its own approved-history selection. The initial and final validation reports are retained; final policy matches the validation minimum in the declared grid.

Offline replays write new output paths and never update online decision audit. To preserve runs durably, upload the versioned manifest through the ADMIN evaluation-run endpoint; duplicate run IDs are rejected. Each report records execution provenance. Existing output files can be regenerated deterministically; use a new output path for candidate experiments.

Observed test performance is limited: the generator intentionally contains missed early card-testing attempts, benign behavioral changes and device-sharing patterns. No real-world accuracy, prevented losses or production throughput is inferred. AUC is deferred until a properly tested tie-aware implementation is justified.
