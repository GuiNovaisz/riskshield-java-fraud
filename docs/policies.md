# Versioned policy

Risk is an ordinal explainable score, not probability of fraud. Sum fired contributions then clamp to [0,100]. Baseline `baseline-1`: 0–29 APPROVE, 30–64 REVIEW, 65–100 BLOCK. Missing evidence upgrades APPROVE to REVIEW; evidence-supported BLOCK remains BLOCK.

Thresholds were selected using the validation cost comparison recorded in `evaluation/calibration/initial-40-70.json`. Demonstration units: FP=10, FN=100, manual REVIEW=2. Minimum-cost pair from the declared grid was 30/65; ties select smaller thresholds. The test period is reported after selection. The block threshold is weakly identified when calibration has no scores above it; it is a conservative grid selection, not proven optimal operational blocking. Real costs require external calibration and analyst capacity constraints.

Cold-start merchant/country novelty is undefined without history and contributes zero; device novelty contributes +18. This avoids mechanically sending every new customer to review, while sparse personal amount baselines do not invent anomaly evidence. The initial calibration predates this explicit cold-start clarification; final validation and test metrics are regenerated and retained. No ML or test-set optimization.

Activation is a serialized PostgreSQL transaction with unique active/shadow indexes, immutable definitions and maker/checker separation. Evaluations persist the complete bundle read at evaluation time; activation cannot retroactively alter prior decisions. Effective dating uses processing/evaluation time to prevent backdated requests selecting retired rules; feature windows use event time.
