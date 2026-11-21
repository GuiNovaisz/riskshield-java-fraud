package io.riskshield.domain;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class GoldenTest {
    record Golden(String id,long velocity,boolean newDevice,boolean amountSpike,long shared,boolean late,int score,DecisionEngine.Outcome decision) {}
    @Test void frozenCalibratedScenarios() throws Exception {
        Golden[] cases=JsonMapper.builder().build().readValue(Files.readString(Path.of("evaluation/golden/scenarios.json")),Golden[].class);
        assertEquals(10,cases.length);
        for(Golden g:cases) {
            Features f=new Features(g.velocity(),g.velocity(),g.velocity(),1,g.shared(),10,g.newDevice(),false,false,g.amountSpike(),1000,g.late()?Set.of("LATE_EVENT"):Set.of(),"features-1");
            var r=new DecisionEngine().evaluate(f,Ruleset.baseline(),Instant.parse("2026-01-01T00:00:00Z"));
            assertEquals(g.score(),r.riskScore(),g.id());assertEquals(g.decision(),r.decision(),g.id());
        }
    }
}
