package io.riskshield.application;

import io.riskshield.adapter.*;
import io.riskshield.domain.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class DecisionService {
    private final JdbcClient db; private final TransactionTemplate tx; private final Json json;
    private final VelocityStore velocity; private final ConfigurationService configs; private final Clock clock; private final Stages stages;
    private final DecisionEngine engine = new DecisionEngine();
    public DecisionService(JdbcClient db,TransactionTemplate tx,Json json,VelocityStore velocity,ConfigurationService configs,Clock clock,Stages stages) {
        this.db=db; this.tx=tx; this.json=json; this.velocity=velocity; this.configs=configs; this.clock=clock; this.stages=stages;
    }
    public DecisionDocument evaluate(Payment payment) {
        return stages.measure("total", () -> evaluateInternal(payment));
    }
    private DecisionDocument evaluateInternal(Payment p) {
        Optional<DecisionDocument> prior=findTransaction(p.transactionId());
        if(prior.isPresent()) {
            if(!prior.get().payment().equals(p)) throw new DuplicateConflict();
            return prior.get();
        }
        Instant now=clock.instant();
        stages.measure("validation",()-> {
            if (p.occurredAt().isAfter(now.plusSeconds(30))) throw new IllegalArgumentException("event over 30 seconds in future");
            if (p.occurredAt().isBefore(now.minus(Duration.ofDays(30)))) throw new IllegalArgumentException("event older than intake retention");
            return true;
        });
        String body=json.write(p), hash=VelocityStore.hash(body);
        // Durable accepted intent precedes Redis. Retry after any failure reuses the same intent.
        tx.executeWithoutResult(status -> {
            db.sql("INSERT INTO intake(transaction_id,payload_hash,payment,customer_id,device_id,merchant_id,country,amount_cents,occurred_at,received_at) VALUES (:id,:h,CAST(:p AS jsonb),:c,:d,:m,:co,:a,:t,:n) ON CONFLICT(transaction_id) DO NOTHING")
                .param("id",p.transactionId()).param("h",hash).param("p",body).param("c",p.customerId()).param("d",p.deviceId()).param("m",p.merchantId()).param("co",p.country()).param("a",p.amountCents()).param("t",utc(p.occurredAt())).param("n",utc(now)).update();
        });
        return tx.execute(status -> {
            Map<String,Object> intent=db.sql("SELECT payload_hash,received_at FROM intake WHERE transaction_id=:id FOR UPDATE").param("id",p.transactionId()).query().singleRow();
            if (!hash.equals(((String)intent.get("payload_hash")).trim())) throw new DuplicateConflict();
            Optional<DecisionDocument> existing=findTransaction(p.transactionId());
            if (existing.isPresent()) return existing.get();
            // Per-entity DB locks coordinate all instances. Sorted lock order prevents cycles.
            java.util.stream.Stream.of("customer:"+p.customerId(),"device:"+p.deviceId()).sorted().forEach(key ->
                db.sql("SELECT pg_advisory_xact_lock(hashtextextended(:k,0))").param("k",key).query((rs,n)->1).list());
            Instant at=clock.instant();
            Features f=stages.measure("features",()->features(p,at));
            Ruleset active=configs.active();
            DecisionEngine.Result result=stages.measure("rules",()->engine.evaluate(f,active,at));
            // Separate scoring span records composition/policy contract without repeating feature work.
            stages.measure("scoring",()->result.riskScore());
            Instant received=((java.sql.Timestamp)intent.get("received_at")).toInstant();
            DecisionDocument doc=new DecisionDocument(UUID.randomUUID(),p.transactionId(),p,f,result,active,received,at);
            stages.measure("persistence",()->db.sql("INSERT INTO decisions(decision_id,transaction_id,document,outcome,score,rule_version,policy_version,evaluated_at) VALUES (:id,:t,CAST(:d AS jsonb),:o,:s,:r,:p,:at)")
                .param("id",doc.decisionId()).param("t",p.transactionId()).param("d",json.write(doc)).param("o",result.decision().name()).param("s",result.riskScore()).param("r",result.ruleVersion()).param("p",result.policyVersion()).param("at",utc(at)).update());
            configs.shadow().filter(c->c.effective(at)).ifPresent(candidate -> {
                DecisionEngine.Result shadow=stages.measure("shadow",()->engine.evaluate(f,candidate,at));
                boolean differs=shadow.decision()!=result.decision();
                db.sql("INSERT INTO shadow_comparisons(decision_id,candidate_version,document,disagreed) VALUES (:d,:v,CAST(:j AS jsonb),:x)")
                    .param("d",doc.decisionId()).param("v",candidate.version()).param("j",json.write(shadow)).param("x",differs).update();
                if(differs) afterCommit(()->stages.count("shadow.disagreements","candidate","shadow"));
            });
            afterCommit(()-> {
                stages.count("decisions","decision",result.decision().name());
                if(result.degraded()) stages.count("degraded.decisions","component","features");
                Set<String> vocabulary=Set.of("NEW_DEVICE","NEW_MERCHANT","NEW_COUNTRY","AMOUNT_SPIKE","VELOCITY_SPIKE","SUSTAINED_VELOCITY","DEVICE_SHARING");
                result.contributions().forEach(r->stages.count("rules.fired","rule",vocabulary.contains(r.reasonCode())?r.reasonCode():"OTHER"));
            });
            return doc;
        });
    }
    private Features features(Payment p,Instant now) {
        Set<String> degraded=new TreeSet<>();
        boolean late=p.occurredAt().isBefore(now.minusSeconds(600));
        long[] v={0,0,0,0,0};
        if(late) degraded.add("LATE_EVENT");
        else {
            try { v=stages.measure("velocity",()->velocity.register(p,now)); }
            catch(RuntimeException unavailable) { degraded.add("VELOCITY_STORE"); stages.count("velocity.errors","type","unavailable"); }
            // Durable lower-bound reconciliation: Redis restart, eviction, pending attempts and partial failures
            // cannot silently undercount. This costs one indexed aggregate; measured before optimization.
            long[] durable=db.sql("SELECT count(*) FILTER(WHERE customer_id=:c AND occurred_at>:m1), count(*) FILTER(WHERE customer_id=:c AND occurred_at>:m5), count(*) FILTER(WHERE customer_id=:c), count(*) FILTER(WHERE device_id=:d AND occurred_at>:m5), count(DISTINCT customer_id) FILTER(WHERE device_id=:d) FROM intake WHERE occurred_at>:h1 AND occurred_at<=:t AND (customer_id=:c OR device_id=:d)")
                .param("c",p.customerId()).param("d",p.deviceId()).param("m1",utc(p.occurredAt().minusSeconds(60))).param("m5",utc(p.occurredAt().minusSeconds(300))).param("h1",utc(p.occurredAt().minusSeconds(3600))).param("t",utc(p.occurredAt()))
                .query((rs,n)->new long[]{rs.getLong(1),rs.getLong(2),rs.getLong(3),rs.getLong(4),rs.getLong(5)}).single();
            for(int i=0;i<5;i++) v[i]=Math.max(v[i],durable[i]);
        }
        List<Payment> history=db.sql("SELECT i.payment::text FROM intake i JOIN decisions d USING(transaction_id) WHERE i.customer_id=:c AND i.occurred_at>=:since AND i.occurred_at<:t AND d.outcome='APPROVE' ORDER BY i.occurred_at DESC,i.transaction_id LIMIT 5001")
            .param("c",p.customerId()).param("since",utc(p.occurredAt().minus(Duration.ofDays(30)))).param("t",utc(p.occurredAt())).query(String.class).list().stream().map(s->json.read(s,Payment.class)).toList();
        if(history.size()>5000) { degraded.add("BEHAVIOR_CAPACITY"); history=history.subList(0,5000); }
        return BehavioralFeatures.compute(p,history,v,degraded);
    }
    public Optional<DecisionDocument> findTransaction(UUID id) { return db.sql("SELECT document::text FROM decisions WHERE transaction_id=:id").param("id",id).query(String.class).optional().map(s->json.read(s,DecisionDocument.class)); }
    public DecisionDocument get(UUID id) { return db.sql("SELECT document::text FROM decisions WHERE decision_id=:id").param("id",id).query(String.class).optional().map(s->json.read(s,DecisionDocument.class)).orElseThrow(()->new NoSuchElementException("decision not found")); }
    public List<DecisionDocument> reviews(int page,int size) {
        if(page<0 || page>10000 || size<1 || size>100) throw new IllegalArgumentException("page 0..10000,size 1..100");
        return db.sql("SELECT document::text FROM decisions WHERE outcome='REVIEW' ORDER BY evaluated_at DESC,decision_id LIMIT :s OFFSET :o").param("s",size).param("o",page*size).query(String.class).list().stream().map(s->json.read(s,DecisionDocument.class)).toList();
    }
    public static OffsetDateTime utc(Instant t) { return t.atOffset(ZoneOffset.UTC); }
    private static void afterCommit(Runnable action) {
        org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(new org.springframework.transaction.support.TransactionSynchronization() {
            @Override public void afterCommit() { action.run(); }
        });
    }
    public static class DuplicateConflict extends RuntimeException { public DuplicateConflict() { super("transaction identity already accepted with different payload"); } }
}
