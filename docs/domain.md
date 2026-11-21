# Domain

`Payment` accepts UUID transaction identity, bounded `syn_` customer/account/merchant/device identifiers, BRL integral cents, country code, WEB/MOBILE/POS and UTC millisecond occurrence time. No PAN, CPF, IP, email or real customer data. Amount range: 1..10,000,000,000 cents. Real currencies require a separate domain revision.

`APPROVE`, `REVIEW`, `BLOCK` are policy outcomes. `BLOCK` is never a fraud label. Labels are FRAUD, LEGITIMATE, UNKNOWN with separate analyst outcomes and delayed observation/recording times. Ruleset, feature version, policy, reason contributions and full configuration snapshot are historical evidence.

Customer/account association is synthetic, supplied by a trusted intake producer. There is no account ledger or real authorization of bank payments. Production ingestion would require authoritative ownership and device provenance.
