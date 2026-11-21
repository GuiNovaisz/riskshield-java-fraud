package io.riskshield.adapter;

import io.riskshield.domain.Payment;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

@Component
public class VelocityStore {
    private final StringRedisTemplate redis;
    @SuppressWarnings("rawtypes") private final DefaultRedisScript<List> script;
    public VelocityStore(StringRedisTemplate redis) {
        this.redis = redis;
        script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("velocity.lua"));
        script.setResultType(List.class);
    }
    public long[] register(Payment p, Instant processingTime) {
        List<?> values = redis.execute(script, List.of(customerKey(p.customerId()), deviceKey(p.deviceId())),
            Long.toString(p.occurredAt().toEpochMilli()), Long.toString(processingTime.toEpochMilli()),
            p.transactionId().toString(), hash(p.customerId()), "4200000", "4500");
        if (values == null || values.size() != 5 || ((Number)values.getFirst()).longValue() < 0) throw new IllegalStateException("velocity capacity exceeded");
        return values.stream().mapToLong(v -> ((Number)v).longValue()).toArray();
    }
    public static String customerKey(String id) { return "rs:v1:c:" + hash(id); }
    public static String deviceKey(String id) { return "rs:v1:d:" + hash(id); }
    public static String hash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (java.security.NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }
}
