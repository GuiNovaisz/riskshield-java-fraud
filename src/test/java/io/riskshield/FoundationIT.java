package io.riskshield;

import io.riskshield.adapter.*;
import io.riskshield.application.*;
import io.riskshield.domain.*;
import static org.junit.jupiter.api.Assertions.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

@SpringBootTest @Testcontainers @TestMethodOrder(MethodOrderer.MethodName.class)
class FoundationIT {
    @Container static final PostgreSQLContainer postgres=new PostgreSQLContainer("postgres:18.1-alpine");
    @Container static final GenericContainer<?> redis=new GenericContainer<>("redis:8.4.0-alpine").withExposedPorts(6379);
    @DynamicPropertySource static void properties(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url",postgres::getJdbcUrl);r.add("spring.datasource.username",postgres::getUsername);r.add("spring.datasource.password",postgres::getPassword);
        r.add("spring.data.redis.host",redis::getHost);r.add("spring.data.redis.port",()->redis.getMappedPort(6379));
        for(String user:List.of("engine","analyst","auditor","admin-a","admin-b")) r.add("risk.users."+user,()->"test-only-password-not-for-deployment");
        r.add("management.tracing.sampling.probability",()->"0");
    }
    @Autowired DecisionService service; @Autowired JdbcClient db; @Autowired StringRedisTemplate template;
    @MockitoSpyBean VelocityStore velocity;
    @Autowired ConfigurationService configs; @Autowired FeedbackService feedback;
    private Payment payment(String c,String d,Instant t,long amount) { return new Payment(UUID.randomUUID(),c,"syn_account",amount,"BRL","syn_merchant",d,"BR",t.truncatedTo(java.time.temporal.ChronoUnit.MILLIS),"WEB"); }
    @Test void idempotencyConflictAndConcurrentDuplicates() throws Exception {
        Payment p=payment("syn_duplicate","syn_dupdevice",Instant.now(),1000);
        try(var pool=Executors.newFixedThreadPool(8)) {
            var tasks=new ArrayList<Callable<DecisionDocument>>(); for(int i=0;i<16;i++) tasks.add(()->service.evaluate(p));
            var results=pool.invokeAll(tasks); UUID id=results.getFirst().get().decisionId();
            for(var result:results) assertEquals(id,result.get().decisionId());
        }
        assertEquals(1L,db.sql("SELECT count(*) FROM decisions WHERE transaction_id=:id").param("id",p.transactionId()).query(Long.class).single());
        assertEquals(1L,template.opsForZSet().zCard(VelocityStore.customerKey(p.customerId())));
        Payment conflict=new Payment(p.transactionId(),p.customerId(),p.accountId(),2000,p.currency(),p.merchantId(),p.deviceId(),p.country(),p.occurredAt(),p.channel());
        assertThrows(DecisionService.DuplicateConflict.class,()->service.evaluate(conflict));
    }
    @Test void redisAtomicWindowsTtlAndOutOfOrder() throws Exception {
        Instant t=Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MILLIS);
        try(var pool=Executors.newFixedThreadPool(8)) {
            List<Callable<long[]>> tasks=new ArrayList<>();for(int i=0;i<64;i++) tasks.add(()->velocity.register(payment("syn_atomic","syn_atomicdevice",t,100),t));
            for(var f:pool.invokeAll(tasks)) f.get();
        }
        String key=VelocityStore.customerKey("syn_atomic");
        assertEquals(64L,template.opsForZSet().zCard(key)); assertTrue(template.getExpire(key)>4400);
        long[] next=velocity.register(payment("syn_atomic","syn_atomicdevice",t.plusSeconds(60),100),t.plusSeconds(60));
        assertEquals(1,next[0]); assertEquals(65,next[1]);
        long[] outOfOrder=velocity.register(payment("syn_atomic","syn_atomicdevice",t.minusSeconds(1),100),t.plusSeconds(60));
        assertEquals(1,outOfOrder[0]);
        assertEquals(66L,template.opsForZSet().zCard(key));
        template.expire(key,Duration.ofMillis(100));
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(3);
        while(Boolean.TRUE.equals(template.hasKey(key)) && System.nanoTime()<deadline) Thread.sleep(25);
        assertFalse(template.hasKey(key));
    }
    @Test void amountDeviceAndLateEventsAreSafe() {
        Instant now=Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MILLIS);
        for(int i=6;i>=1;i--) service.evaluate(payment("syn_behavior","syn_known",now.minusSeconds(i*60L),1000));
        var spike=service.evaluate(payment("syn_behavior","syn_fresh",now,5000));
        assertTrue(spike.features().newDevice()); assertTrue(spike.features().amountSpike());
        var late=service.evaluate(payment("syn_behavior","syn_latedevice",now.minusSeconds(1000),1000));
        assertTrue(late.result().degraded()); assertTrue(late.features().degradedComponents().contains("LATE_EVENT"));
        assertEquals(0,late.features().priorPayments());
    }
    @Test void immutableAuditFeedbackAndLabelLeakage() {
        var doc=service.evaluate(payment("syn_feedback","syn_feedbackdevice",Instant.now().minusSeconds(2),1000));
        feedback.record(new FeedbackService.Feedback(UUID.randomUUID(),doc.decisionId(),FeedbackService.Label.FRAUD,FeedbackService.ManualOutcome.CONFIRMED_FRAUD,doc.evaluatedAt(),"synthetic analyst finding"),"analyst");
        assertEquals(doc,service.get(doc.decisionId()));
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->db.sql("UPDATE decisions SET score=100 WHERE decision_id=:id").param("id",doc.decisionId()).update());
        assertThrows(IllegalArgumentException.class,()->feedback.record(new FeedbackService.Feedback(UUID.randomUUID(),doc.decisionId(),FeedbackService.Label.FRAUD,FeedbackService.ManualOutcome.CONFIRMED_FRAUD,doc.evaluatedAt().minusSeconds(1),"too early"),"analyst"));
    }
    @Test void zGovernanceMakerCheckerShadowAndVersionHistory() {
        var base=Ruleset.baseline();String version="candidate-"+UUID.randomUUID().toString().substring(0,8);
        var rules=new ArrayList<>(base.rules());var first=rules.getFirst();
        rules.set(0,new Ruleset.Rule(first.id(),"2",first.description(),true,first.priority(),first.signal(),first.threshold(),50,first.effectiveFrom(),first.effectiveUntil()));
        var candidate=new Ruleset(version,base.effectiveFrom(),base.effectiveUntil(),1,99,rules);
        configs.draft(candidate,"admin-a","synthetic candidate");
        assertThrows(ConfigurationService.GovernanceConflict.class,()->configs.transition(version,"APPROVED","admin-a","self approval"));
        configs.transition(version,"APPROVED","admin-b","independent review");configs.transition(version,"SHADOW","admin-b","shadow experiment");
        var doc=service.evaluate(payment("syn_shadow","syn_shadowdevice",Instant.now(),1000));
        assertEquals("baseline-1",doc.result().policyVersion());assertEquals(DecisionEngine.Outcome.APPROVE,doc.result().decision());
        assertTrue(db.sql("SELECT disagreed FROM shadow_comparisons WHERE decision_id=:id").param("id",doc.decisionId()).query(Boolean.class).single());
        assertEquals(doc,service.get(doc.decisionId()));
        configs.transition(version,"ACTIVE","admin-b","promote reviewed candidate");
        var fresh=service.evaluate(payment("syn_newconfig","syn_newconfigdevice",Instant.now(),1000));
        assertEquals(version,fresh.result().policyVersion());assertEquals("2",fresh.result().contributions().getFirst().ruleVersion());
        assertEquals(doc,service.get(doc.decisionId()));assertEquals("baseline-1",doc.configuration().version());
    }
    @Test void concurrentUniqueAttemptsKeepAllAuditAndVelocity() throws Exception {
        Instant t=Instant.now();
        try(var pool=Executors.newFixedThreadPool(8)) {
            List<Callable<DecisionDocument>> tasks=new ArrayList<>();for(int i=0;i<32;i++) {Payment p=payment("syn_uniqueconcurrent","syn_uniqueconcurrentdevice",t,1000);tasks.add(()->service.evaluate(p));}
            List<DecisionDocument> docs=new ArrayList<>();for(var r:pool.invokeAll(tasks)) docs.add(r.get());
            assertEquals(32,docs.stream().map(DecisionDocument::decisionId).distinct().count());assertEquals(32,docs.stream().mapToLong(d->d.features().customer1m()).max().orElseThrow());
        }
        assertEquals(32L,template.opsForZSet().zCard(VelocityStore.customerKey("syn_uniqueconcurrent")));
        assertEquals(32L,db.sql("SELECT count(*) FROM decisions d JOIN intake i USING(transaction_id) WHERE i.customer_id='syn_uniqueconcurrent'").query(Long.class).single());
    }
    @Test void redisOutageAndDatabaseFailureContracts() {
        org.mockito.Mockito.doThrow(new IllegalStateException("injected Redis failure")).when(velocity).register(org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any());
        try {
            var doc=service.evaluate(payment("syn_outage","syn_outagedevice",Instant.now(),1000));
            assertEquals(DecisionEngine.Outcome.REVIEW,doc.result().decision());assertTrue(doc.result().degraded());
        } finally { org.mockito.Mockito.doCallRealMethod().when(velocity).register(org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any()); }
        // Deterministic database-side failure after features, before response; no fake persisted success.
        db.sql("CREATE FUNCTION fail_decision_test() RETURNS trigger LANGUAGE plpgsql AS 'BEGIN RAISE EXCEPTION ''injected write failure''; END'").update();
        db.sql("CREATE TRIGGER fail_decision_test BEFORE INSERT ON decisions FOR EACH ROW EXECUTE FUNCTION fail_decision_test()").update();
        Payment p=payment("syn_dbfailure","syn_dbfailuredevice",Instant.now(),1000);
        try { assertThrows(org.springframework.dao.DataAccessException.class,()->service.evaluate(p));assertTrue(service.findTransaction(p.transactionId()).isEmpty()); }
        finally { db.sql("DROP TRIGGER fail_decision_test ON decisions").update();db.sql("DROP FUNCTION fail_decision_test()").update(); }
        var retried=service.evaluate(p);assertNotNull(retried.decisionId());assertEquals(1L,template.opsForZSet().zCard(VelocityStore.customerKey(p.customerId())));
    }
}
