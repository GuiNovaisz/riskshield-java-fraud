package io.riskshield.evaluation;

import static org.junit.jupiter.api.Assertions.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;

class EvaluationTest {
    @org.junit.jupiter.api.io.TempDir java.nio.file.Path temp;
    @Test void independentJvmReportsAreByteReproducible() throws Exception {
        String executable=java.nio.file.Path.of(System.getProperty("java.home"),"bin",System.getProperty("os.name").startsWith("Windows")?"java.exe":"java").toString();
        String cp=System.getProperty("surefire.test.class.path",System.getProperty("java.class.path"));
        for(String name:List.of("a","b")) {
            var p=new ProcessBuilder(executable,"-Dapplication.commit=test","-Devaluation.at=2026-11-21T00:00:00Z","-cp",cp,EvaluationMain.class.getName(),"20261121",temp.resolve(name).toString())
                .redirectErrorStream(true).redirectOutput(temp.resolve(name+".log").toFile()).start();
            assertTrue(p.waitFor(30,java.util.concurrent.TimeUnit.SECONDS));assertEquals(0,p.exitValue(),java.nio.file.Files.readString(temp.resolve(name+".log")));
        }
        assertArrayEquals(java.nio.file.Files.readAllBytes(temp.resolve("a/evaluation.json")),java.nio.file.Files.readAllBytes(temp.resolve("b/evaluation.json")));
        assertArrayEquals(java.nio.file.Files.readAllBytes(temp.resolve("a/dataset.jsonl")),java.nio.file.Files.readAllBytes(temp.resolve("b/dataset.jsonl")));
    }
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
