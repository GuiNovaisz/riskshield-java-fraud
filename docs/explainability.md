# Explainability

`result.contributions` lists stable reason code, ruleVersion, observed feature value, comparator threshold and signed points. `rawScore` is the sum; `riskScore` is the clamp. Configuration records thresholds and full rule definitions. Features record degraded components and version. Even APPROVE with no fired rules is explained by score=0, empty contribution list and the preserved policy/feature snapshot.

REVIEW with score 0 can be caused by missing evidence; it is not scored as fake fraud. No score is a calibrated fraud probability. Country novelty is not impossible travel. BLOCK is a policy action only.
