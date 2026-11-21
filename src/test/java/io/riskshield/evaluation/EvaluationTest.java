package io.riskshield.evaluation;

import static org.junit.jupiter.api.Assertions.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;

class EvaluationTest {
    @Test void deterministicSeedImbalanceAndLabelIsolation() {
        var a=EvaluationMain.generate(42,50,40);var b=EvaluationMain.generate(42,50,40);assertEquals(a,b);
        double prevalence=(double)a.stream().filter(EvaluationMain.Row::fraud).count()/a.size();assertTrue(prevalence>0 && prevalence<0.15);
        var scored=EvaluationMain.replay(a);
        var poisoned=EvaluationMain.replay(a.stream().map(r->new EvaluationMain.Row(r.payment(),r.scenario(),!r.fraud(),Instant.MAX)).toList());
        for(int i=0;i<scored.size();i++) {assertEquals(scored.get(i).features(),poisoned.get(i).features());assertEquals(scored.get(i).result(),poisoned.get(i).result());}
    }
    @Test void confusionMatrixArithmeticOnKnownCases() {
        var rows=EvaluationMain.generate(123,20,12);var scored=EvaluationMain.replay(rows);
        var m=EvaluationMain.metrics(scored,1,90,new EvaluationMain.Costs(10,100,2));
        @SuppressWarnings("unchecked") var matrix=(Map<String,Long>)m.get("confusionMatrix");assertEquals(rows.size(),matrix.values().stream().mapToLong(Long::longValue).sum());
        long positives=matrix.get("TP")+matrix.get("FN");
        assertEquals(positives==0?0:(double)matrix.get("TP")/positives,((Number)m.get("recall")).doubleValue());
    }
}
