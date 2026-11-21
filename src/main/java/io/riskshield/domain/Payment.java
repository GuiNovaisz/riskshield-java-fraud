package io.riskshield.domain;

import java.time.Instant;
import java.util.UUID;

/** Synthetic identifiers only. Money is integral BRL cents, never floating point. */
public record Payment(UUID transactionId, String customerId, String accountId, long amountCents,
                      String currency, String merchantId, String deviceId, String country,
                      Instant occurredAt, String channel) {
    public Payment {
        if (transactionId == null || occurredAt == null) throw new IllegalArgumentException("identity/time required");
        for (String id : new String[]{customerId, accountId, merchantId, deviceId}) {
            if (id == null || !id.matches("syn_[A-Za-z0-9_-]{1,64}"))
                throw new IllegalArgumentException("only bounded syn_ identifiers accepted");
        }
        if (amountCents <= 0 || amountCents > 100_000_000_00L) throw new IllegalArgumentException("invalid amount cents");
        if (!"BRL".equals(currency)) throw new IllegalArgumentException("BRL only");
        if (country == null || !country.matches("[A-Z]{2}")) throw new IllegalArgumentException("invalid country");
        if (!java.util.Set.of("WEB", "MOBILE", "POS").contains(channel == null ? "" : channel))
            throw new IllegalArgumentException("invalid channel");
        if (occurredAt.getNano() % 1_000_000 != 0) throw new IllegalArgumentException("millisecond precision required");
    }
}
