package io.riskshield.adapter;

import io.riskshield.application.*;
import io.riskshield.domain.*;
import java.security.Principal;
import java.util.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/risk")
public class RiskController {
    private final DecisionService decisions; private final FeedbackService feedback; private final ConfigurationService configs; private final JdbcClient db; private final EvaluationRunService runs;
    public RiskController(DecisionService decisions,FeedbackService feedback,ConfigurationService configs,JdbcClient db,EvaluationRunService runs) { this.decisions=decisions; this.feedback=feedback; this.configs=configs; this.db=db; this.runs=runs; }
    @PostMapping("/evaluations") public DecisionDocument evaluate(@RequestBody Payment p) { return decisions.evaluate(p); }
    @GetMapping("/decisions/{id}") public DecisionDocument decision(@PathVariable UUID id) { return decisions.get(id); }
    @GetMapping("/reviews") public List<DecisionDocument> reviews(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="25") int size) { return decisions.reviews(page,size); }
    @PostMapping("/feedback") public Map<String,String> feedback(@RequestBody FeedbackService.Feedback f,Principal actor) { feedback.record(f,actor.getName()); return Map.of("status","recorded"); }
    public record Draft(Ruleset configuration,String reason) {}
    public record Transition(String target,String reason) {}
    @PostMapping("/admin/configurations") public Map<String,String> draft(@RequestBody Draft d,Principal actor) { if(d.configuration()==null)throw new IllegalArgumentException("configuration required");configs.draft(d.configuration(),actor.getName(),d.reason()); return Map.of("status","DRAFT"); }
    @PostMapping("/admin/configurations/{version}/transitions") public Map<String,String> transition(@PathVariable String version,@RequestBody Transition t,Principal actor) { configs.transition(version,t.target(),actor.getName(),t.reason()); return Map.of("status",t.target()); }
    @GetMapping("/audit/governance") public List<Map<String,Object>> audit(@RequestParam(defaultValue="0") int page) {
        checkPage(page); return db.sql("SELECT actor,action,version,before_state,after_state,reason,occurred_at FROM admin_audit ORDER BY audit_id DESC LIMIT 50 OFFSET :o").param("o",page*50).query().listOfRows();
    }
    @GetMapping("/audit/shadows") public List<Map<String,Object>> shadows(@RequestParam(defaultValue="0") int page) {
        checkPage(page); return db.sql("SELECT decision_id,candidate_version,document,disagreed FROM shadow_comparisons ORDER BY decision_id LIMIT 50 OFFSET :o").param("o",page*50).query().listOfRows();
    }
    private void checkPage(int p) { if(p<0 || p>10000) throw new IllegalArgumentException("invalid page"); }
    @PostMapping("/admin/evaluation-runs") public Map<String,String> evaluationRun(@RequestBody EvaluationRunService.Manifest manifest,Principal actor) {
        runs.record(manifest,actor.getName());return Map.of("status","recorded");
    }
}
