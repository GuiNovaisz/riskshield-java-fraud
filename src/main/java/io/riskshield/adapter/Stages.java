package io.riskshield.adapter;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

@Component
public class Stages {
    private final MeterRegistry meters;
    private final ObservationRegistry observations;
    public Stages(MeterRegistry meters, ObservationRegistry observations) { this.meters=meters; this.observations=observations; }
    public <T> T measure(String stage, Supplier<T> action) {
        return Observation.createNotStarted("risk." + stage, observations).observe(() ->
            meters.timer("risk.stage", "stage", stage).record(action));
    }
    public void count(String name, String key, String value) { meters.counter("risk."+name, key, value).increment(); }
}
