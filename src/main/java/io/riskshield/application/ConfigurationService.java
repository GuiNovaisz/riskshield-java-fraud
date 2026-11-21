package io.riskshield.application;

import io.riskshield.adapter.Json;
import io.riskshield.domain.Ruleset;
import java.time.Clock;
import java.util.*;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class ConfigurationService implements ApplicationRunner {
    private final JdbcClient db; private final TransactionTemplate tx; private final Json json; private final Clock clock;
    public ConfigurationService(JdbcClient db, TransactionTemplate tx, Json json, Clock clock) { this.db=db; this.tx=tx; this.json=json; this.clock=clock; }
    @Override public void run(ApplicationArguments args) {
        tx.executeWithoutResult(status -> {
            lock();
            if (db.sql("SELECT count(*) FROM rulesets").query(Long.class).single() == 0) {
                Ruleset baseline = Ruleset.baseline();
                db.sql("INSERT INTO rulesets(version,definition,maker) VALUES (:v,CAST(:d AS jsonb),'bootstrap')").param("v",baseline.version()).param("d",json.write(baseline)).update();
                db.sql("INSERT INTO config_state(version,state,checker) VALUES (:v,'ACTIVE','bootstrap-reviewed-in-source')").param("v",baseline.version()).update();
                audit("bootstrap",baseline.version(),"BOOTSTRAP",null,"ACTIVE","Source-reviewed baseline; thresholds require synthetic evaluation");
            }
        });
    }
    private void lock() { db.sql("SELECT pg_advisory_xact_lock(780001)").query((rs,n)->1).list(); }
    public Ruleset active() { return config("ACTIVE").orElseThrow(()->new IllegalStateException("no active configuration")); }
    public Optional<Ruleset> shadow() { return config("SHADOW"); }
    private Optional<Ruleset> config(String state) {
        return db.sql("SELECT definition::text FROM rulesets r JOIN config_state s USING(version) WHERE s.state=:s").param("s",state).query(String.class).optional().map(s->json.read(s,Ruleset.class));
    }
    public void draft(Ruleset rules, String actor, String reason) {
        validateReason(reason);
        tx.executeWithoutResult(status -> { lock();
            if (!rules.effectiveUntil().isAfter(clock.instant())) throw new IllegalArgumentException("configuration already expired");
            db.sql("INSERT INTO rulesets(version,definition,maker) VALUES (:v,CAST(:d AS jsonb),:a)").param("v",rules.version()).param("d",json.write(rules)).param("a",actor).update();
            db.sql("INSERT INTO config_state(version,state) VALUES (:v,'DRAFT')").param("v",rules.version()).update();
            audit(actor,rules.version(),"CREATE",null,"DRAFT",reason);
        });
    }
    public void transition(String version, String target, String actor, String reason) {
        validateReason(reason);
        tx.executeWithoutResult(status -> { lock();
            Map<String,Object> row=db.sql("SELECT s.state,r.maker,s.checker,r.definition::text FROM config_state s JOIN rulesets r USING(version) WHERE version=:v FOR UPDATE OF s")
                .param("v",version).query((rs,n)-> {
                    Map<String,Object> m=new HashMap<>();
                    for(String key:List.of("state","maker","checker","definition")) m.put(key,rs.getString(key));
                    return m;
                }).optional().orElseThrow(()->new NoSuchElementException("configuration not found"));
            String before=(String)row.get("state"), maker=(String)row.get("maker");
            if ("APPROVED".equals(target)) {
                if (!"DRAFT".equals(before) || maker.equals(actor)) throw new GovernanceConflict("independent checker required for DRAFT");
                db.sql("UPDATE config_state SET checker=:a,state='APPROVED' WHERE version=:v").param("a",actor).param("v",version).update();
            } else if (Set.of("SHADOW","ACTIVE").contains(target)) {
                if (!Set.of("APPROVED","SHADOW").contains(before) || maker.equals(actor)) throw new GovernanceConflict("independent checker and approved configuration required");
                Ruleset config=json.read((String)row.get("definition"),Ruleset.class);
                if (!config.effective(clock.instant())) throw new IllegalArgumentException("configuration is not effective now");
                for (String old:db.sql("SELECT version FROM config_state WHERE state=:s AND version<>:v").param("s",target).param("v",version).query(String.class).list()) {
                    db.sql("UPDATE config_state SET state='RETIRED' WHERE version=:v").param("v",old).update();
                    audit(actor,old,"RETIRE",target,"RETIRED",reason);
                }
                db.sql("UPDATE config_state SET state=:s WHERE version=:v").param("s",target).param("v",version).update();
            } else throw new IllegalArgumentException("allowed targets APPROVED,SHADOW,ACTIVE");
            audit(actor,version,"TRANSITION",before,target,reason);
        });
    }
    private void audit(String actor,String version,String action,String before,String after,String reason) {
        db.sql("INSERT INTO admin_audit(actor,action,version,before_state,after_state,reason,occurred_at) VALUES (:a,:op,:v,:b,:s,:r,:t)")
            .param("a",actor).param("op",action).param("v",version).param("b",before).param("s",after).param("r",reason).param("t",clock.instant().atOffset(java.time.ZoneOffset.UTC)).update();
    }
    public static void validateReason(String reason) { if (reason==null || reason.isBlank() || reason.length()>240) throw new IllegalArgumentException("reason must be 1..240 characters"); }
    public static class GovernanceConflict extends RuntimeException { public GovernanceConflict(String m) { super(m); } }
}
