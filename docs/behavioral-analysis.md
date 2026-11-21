# Behavioral baseline

Use approved decisions already committed at evaluation, associated synthetic customer, occurrence in [t-30d,t), deterministic event ordering, maximum 5000 rows. Overflow marks BEHAVIOR_CAPACITY and forces at least REVIEW. The aggregate is recomputed from bounded history rather than mutating a last-device/location singleton, so out-of-order inserts cannot overwrite a newer location/device. There is no last-location travel feature in this release.

Baseline excludes current and future occurrences, blocked/reviewed events and all labels. Minimum five previous approvals for amount anomaly. New merchant/country require nonempty history; new device is true at cold start. Small history, legitimate travel/device replacement, and high-spend habits are represented in synthetic data.

Approval selection can bias baselines; approved fraud may poison history, and a reviewed device does not become trusted automatically. These are deliberate documented limitations; a production profile would require authoritative feedback and governance. No claim that synthetically supplied devices are trustworthy.
