package io.riskshield.domain;

import java.util.Set;

public record Features(long customer1m, long customer5m, long customer1h, long device5m,
                       long customersOnDevice1h, long priorPayments, boolean newDevice,
                       boolean newMerchant, boolean newCountry, boolean amountSpike,
                       long historicalMeanCents, Set<String> degradedComponents, String version) {
    public Features { degradedComponents = Set.copyOf(degradedComponents); }
    public long value(Signal signal) {
        return switch (signal) {
            case CUSTOMER_1M -> customer1m;
            case CUSTOMER_5M -> customer5m;
            case DEVICE_SHARING -> customersOnDevice1h;
            case NEW_DEVICE -> newDevice ? 1 : 0;
            case NEW_MERCHANT -> newMerchant ? 1 : 0;
            case NEW_COUNTRY -> newCountry ? 1 : 0;
            case AMOUNT_SPIKE -> amountSpike ? 1 : 0;
        };
    }
    public enum Signal { CUSTOMER_1M, CUSTOMER_5M, DEVICE_SHARING, NEW_DEVICE, NEW_MERCHANT, NEW_COUNTRY, AMOUNT_SPIKE }
}
