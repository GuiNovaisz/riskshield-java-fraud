# Dataset provenance

Entirely synthetic `generator-1`, seeded java.util.Random; default seed 20261121. 200 synthetic customers over 40 rounds; five recurring profiles LOW_SPEND, HIGH_SPEND, TRAVELER, NIGHT_USER, NEW_CUSTOMER. Profile names denote intended diversity; NIGHT_USER is a label only (usual-hour features are not implemented), and NEW_CUSTOMER shares the cold-start construction. Each event is fictitious BRL integral cents with synthetic identifiers and country codes.

Attack initiation after eight warmup rounds: probability 0.012, equally sampled ACCOUNT_TAKEOVER, CARD_TESTING (8 attempts), DEVICE_SHARING. Benign device/amount changes probability 0.025 after warmup. Bursts change event-level fraud prevalence; report actual counts rather than equating initiation probability to prevalence. Fraud labels describe generator intent, not independently verified financial fraud.

Default output: 8168 events; actual scenario distribution and prevalence in evaluation.json. Label availability is event +7 days. Dataset sha256 in run manifest. No external banking dataset, no proprietary data. Main evaluation is imbalanced; golden fixtures deliberately target boundary cases and are not used to claim accuracy. Synthetic rules/scenarios are correlated by design; transfer to real data is unknown.
