package io.riskshield.domain;

import static org.junit.jupiter.api.Assertions.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class DecisionEngineTest {
    private final DecisionEngine engine=new DecisionEngine();
    private final Instant at=Instant.parse("2026-01-01T00:00:00Z");
    private Features f(long velocity,boolean fresh,boolean spike,Set<String> degraded) {
        return new Features(velocity,velocity,velocity,1,1,10,fresh,false,false,spike,1000,degraded,"features-1");
    }
    @Test void deterministicExplainableAndBounded() {
        var random=new Random(42);
        for(int i=0;i<1000;i++) {
            Features f=f(random.nextInt(100),random.nextBoolean(),random.nextBoolean(),Set.of());
            var a=engine.evaluate(f,Ruleset.baseline(),at); var b=engine.evaluate(f,Ruleset.baseline(),at);
            assertEquals(a,b); assertTrue(a.riskScore()>=0 && a.riskScore()<=100);
            assertEquals(Math.clamp(a.contributions().stream().mapToInt(DecisionEngine.Contribution::points).sum(),0,100),a.riskScore());
        }
    }
    @Test void ruleEvasionAndCombinedSignals() {
        assertEquals(DecisionEngine.Outcome.APPROVE,engine.evaluate(f(1,true,false,Set.of()),Ruleset.baseline(),at).decision());
        assertEquals(DecisionEngine.Outcome.REVIEW,engine.evaluate(f(1,true,true,Set.of()),Ruleset.baseline(),at).decision());
        assertEquals(DecisionEngine.Outcome.BLOCK,engine.evaluate(f(12,true,true,Set.of()),Ruleset.baseline(),at).decision());
    }
    @Test void unavailableEvidenceNeverApproves() {
        assertEquals(DecisionEngine.Outcome.REVIEW,engine.evaluate(f(0,false,false,Set.of("VELOCITY_STORE")),Ruleset.baseline(),at).decision());
    }
    @Test void effectiveDatingAndValidation() {
        assertThrows(IllegalStateException.class,()->engine.evaluate(f(1,false,false,Set.of()),Ruleset.baseline(),Instant.parse("2101-01-01T00:00:00Z")));
        var b=Ruleset.baseline();
        assertThrows(IllegalArgumentException.class,()->new Ruleset("bad",b.effectiveFrom(),b.effectiveUntil(),70,40,b.rules()));
        assertThrows(IllegalArgumentException.class,()->new Ruleset("bad",b.effectiveFrom(),b.effectiveUntil(),40,70,List.of(b.rules().getFirst(),b.rules().getFirst())));
    }
    @Test void behavioralTimeFilteringAndNoLabelInput() {
        Payment current=p("syn_new",at,4000); List<Payment> prior=new ArrayList<>();
        for(int i=1;i<=5;i++) prior.add(p("syn_known",at.minusSeconds(i*60L),1000));
        prior.add(p("syn_new",at.plusSeconds(60),100000));
        var f=BehavioralFeatures.compute(current,prior,new long[]{1,1,1,1,1},Set.of());
        assertEquals(5,f.priorPayments()); assertTrue(f.newDevice()); assertTrue(f.amountSpike()); assertEquals(1000,f.historicalMeanCents());
        assertFalse(BehavioralFeatures.compute(p("syn_known",at,1000),prior,new long[]{1,1,1,1,1},Set.of()).newDevice());
        assertEquals(f,BehavioralFeatures.compute(current,prior.reversed(),new long[]{1,1,1,1,1},Set.of()));
    }
    private Payment p(String d,Instant t,long amount) { return new Payment(UUID.nameUUIDFromBytes((d+t).getBytes(java.nio.charset.StandardCharsets.UTF_8)),"syn_c","syn_a",amount,"BRL","syn_m",d,"BR",t,"WEB"); }
}
