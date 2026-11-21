package io.riskshield.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.*;

public final class BehavioralFeatures {
    private BehavioralFeatures() {}
    public static Features compute(Payment p,List<Payment> availableApprovedHistory,long[] velocity,Set<String> degraded) {
        List<Payment> prior=availableApprovedHistory.stream().filter(h->h.customerId().equals(p.customerId()) && h.occurredAt().isBefore(p.occurredAt()) && !h.occurredAt().isBefore(p.occurredAt().minus(Duration.ofDays(30)))).toList();
        BigDecimal sum=prior.stream().map(h->BigDecimal.valueOf(h.amountCents())).reduce(BigDecimal.ZERO,BigDecimal::add);
        long mean=prior.isEmpty()?0:sum.divide(BigDecimal.valueOf(prior.size()),0,RoundingMode.HALF_UP).longValueExact();
        boolean spike=prior.size()>=5 && BigDecimal.valueOf(p.amountCents()).multiply(BigDecimal.valueOf(prior.size())).compareTo(sum.multiply(BigDecimal.valueOf(3)))>0;
        return new Features(velocity[0],velocity[1],velocity[2],velocity[3],velocity[4],prior.size(),
            prior.stream().noneMatch(h->h.deviceId().equals(p.deviceId())),!prior.isEmpty() && prior.stream().noneMatch(h->h.merchantId().equals(p.merchantId())),
            !prior.isEmpty() && prior.stream().noneMatch(h->h.country().equals(p.country())),spike,mean,degraded,"features-1");
    }
}
