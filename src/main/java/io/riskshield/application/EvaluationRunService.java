package io.riskshield.application;

import io.riskshield.adapter.Json;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class EvaluationRunService {
    public record Manifest(UUID runId,String datasetVersion,String generatorVersion,long seed,long size,
                           String datasetSha256,String reportSha256,String ruleVersion,String policyVersion,
                           String modelVersion,String applicationCommit,Instant reportedAt,String reason) {}
    private final JdbcClient db; private final Json json; private final Clock clock; private final TransactionTemplate tx;
    public EvaluationRunService(JdbcClient db,Json json,Clock clock,TransactionTemplate tx) {this.db=db;this.json=json;this.clock=clock;this.tx=tx;}
    public void record(Manifest m,String actor) {
        if(m.runId()==null || m.size()<1 || m.size()>10_000_000 || m.reportedAt()==null || m.reportedAt().isAfter(clock.instant()))throw new IllegalArgumentException("invalid run metadata");
        for(String v:new String[]{m.datasetVersion(),m.generatorVersion(),m.ruleVersion(),m.policyVersion()}) if(v==null || !v.matches("[a-zA-Z0-9._-]{1,48}"))throw new IllegalArgumentException("invalid version");
        for(String hash:new String[]{m.datasetSha256(),m.reportSha256()})if(hash==null || !hash.matches("[0-9a-f]{64}"))throw new IllegalArgumentException("SHA256 required");
        if(!"NONE".equals(m.modelVersion()) || m.applicationCommit()==null || !m.applicationCommit().matches("[0-9a-f]{40}"))throw new IllegalArgumentException("model NONE and Git commit required");
        ConfigurationService.validateReason(m.reason());
        tx.executeWithoutResult(status -> {
            if(db.sql("SELECT count(*) FROM rulesets WHERE version IN (:r,:p)").param("r",m.ruleVersion()).param("p",m.policyVersion()).query(Long.class).single() < (m.ruleVersion().equals(m.policyVersion())?1:2))throw new IllegalArgumentException("unknown configuration version");
            db.sql("INSERT INTO evaluation_runs(run_id,manifest) VALUES (:id,CAST(:m AS jsonb))").param("id",m.runId()).param("m",json.write(Map.of("manifest",m,"actor",actor,"recordedAt",clock.instant()))).update();
        });
    }
}
