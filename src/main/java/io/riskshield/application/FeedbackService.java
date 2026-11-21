package io.riskshield.application;

import io.riskshield.adapter.Stages;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class FeedbackService {
    public enum Label { FRAUD, LEGITIMATE, UNKNOWN }
    public enum ManualOutcome { CONFIRMED_FRAUD, LEGITIMATE, INCONCLUSIVE }
    public record Feedback(UUID feedbackId,UUID decisionId,Label label,ManualOutcome outcome,Instant observedAt,String reason) {}
    private final JdbcClient db; private final DecisionService decisions; private final Clock clock; private final TransactionTemplate tx; private final Stages stages;
    public FeedbackService(JdbcClient db,DecisionService decisions,Clock clock,TransactionTemplate tx,Stages stages) { this.db=db; this.decisions=decisions; this.clock=clock; this.tx=tx; this.stages=stages; }
    public void record(Feedback f,String actor) {
        if(f.feedbackId()==null || f.decisionId()==null || f.label()==null || f.outcome()==null || f.observedAt()==null) throw new IllegalArgumentException("feedback identity,label,outcome,time required");
        ConfigurationService.validateReason(f.reason());
        Label expected=switch(f.outcome()) { case CONFIRMED_FRAUD -> Label.FRAUD; case LEGITIMATE -> Label.LEGITIMATE; case INCONCLUSIVE -> Label.UNKNOWN; };
        if(f.label()!=expected) throw new IllegalArgumentException("label/outcome mismatch");
        tx.executeWithoutResult(status -> {
            DecisionDocument doc=decisions.get(f.decisionId());
            if(f.observedAt().isBefore(doc.evaluatedAt()) || f.observedAt().isAfter(clock.instant())) throw new IllegalArgumentException("feedback must be observed after decision and no later than now");
            db.sql("INSERT INTO feedback(feedback_id,decision_id,label,outcome,actor,reason,observed_at) VALUES (:i,:d,:l,:o,:a,:r,:t)")
                .param("i",f.feedbackId()).param("d",f.decisionId()).param("l",f.label().name()).param("o",f.outcome().name()).param("a",actor).param("r",f.reason()).param("t",DecisionService.utc(f.observedAt())).update();
            stages.count("feedback","label",f.label().name());
        });
    }
}
