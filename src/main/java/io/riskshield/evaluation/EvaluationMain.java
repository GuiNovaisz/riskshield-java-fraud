package io.riskshield.evaluation;

import io.riskshield.domain.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import tools.jackson.databind.json.JsonMapper;

/** Offline replay uses the production domain engine and behavioral definitions, never labels as features. */
public final class EvaluationMain {
    public record Row(Payment payment,String scenario,boolean fraud,Instant labelAvailableAt) {}
    public record Scored(Row row,Features features,DecisionEngine.Result result) {}
    public record Costs(long falsePositive,long falseNegative,long review) {}
    private static final Instant START=Instant.parse("2026-01-01T00:00:00Z");
    public static List<Row> generate(long seed,int customers,int rounds) {
        Random random=new Random(seed);List<Row> rows=new ArrayList<>();int serial=0;
        for(int round=0;round<rounds;round++) for(int c=0;c<customers;c++) {
            String profile=switch(c%5) {case 0->"LOW_SPEND";case 1->"HIGH_SPEND";case 2->"TRAVELER";case 3->"NIGHT_USER";default->"NEW_CUSTOMER";};
            Instant t=START.plusSeconds(round*3600L+c*3L);
            long base=c%5==1?200000:2000;
            long amount=base+random.nextInt((int)base);
            boolean fraud=round>=8 && random.nextDouble()<0.012;
            boolean benignNovel=round>=8 && random.nextDouble()<0.025;
            String device="syn_d"+c, merchant="syn_m"+(c%10), country="BR",scenario=profile;
            if(c%5==2 && round%10==9) country="PT";
            if(benignNovel) { device+="_new"+round; amount*=4; scenario="LEGITIMATE_CHANGE"; }
            int burst=1;
            if(fraud) {
                switch(random.nextInt(3)) {
                    case 0 -> { device+="_takeover"+round;amount*=8;scenario="ACCOUNT_TAKEOVER"; }
                    case 1 -> { amount=100;burst=8;scenario="CARD_TESTING";device+="_testing"; }
                    default -> { device="syn_shared_attack";amount*=5;merchant="syn_unusual";scenario="DEVICE_SHARING"; }
                }
            }
            for(int b=0;b<burst;b++) {
                String identity=seed+":"+(serial++);
                Payment p=new Payment(UUID.nameUUIDFromBytes(identity.getBytes(StandardCharsets.UTF_8)),"syn_c"+c,"syn_a"+c,amount,"BRL",merchant,device,country,t.plusSeconds(b*3L),"WEB");
                rows.add(new Row(p,scenario,fraud,p.occurredAt().plus(Duration.ofDays(7))));
            }
        }
        rows.sort(Comparator.comparing((Row r)->r.payment().occurredAt()).thenComparing(r->r.payment().transactionId()));return List.copyOf(rows);
    }
    public static List<Scored> replay(List<Row> rows) {
        return replay(rows,Ruleset.baseline());
    }
    public static List<Scored> replay(List<Row> rows,Ruleset config) {
        Map<String,List<Payment>> acceptedCustomer=new HashMap<>(),acceptedDevice=new HashMap<>(),approved=new HashMap<>();
        List<Scored> scored=new ArrayList<>();DecisionEngine engine=new DecisionEngine();
        for(Row row:rows) {
            Payment p=row.payment();
            List<Payment> ch=acceptedCustomer.computeIfAbsent(p.customerId(),k->new ArrayList<>()), dh=acceptedDevice.computeIfAbsent(p.deviceId(),k->new ArrayList<>());
            ch.add(p);dh.add(p);
            long[] v={count(ch,p,60),count(ch,p,300),count(ch,p,3600),count(dh,p,300),dh.stream().filter(h->inWindow(h,p,3600)).map(Payment::customerId).distinct().count()};
            Features f=BehavioralFeatures.compute(p,approved.getOrDefault(p.customerId(),List.of()),v,Set.of());
            var result=engine.evaluate(f,config,p.occurredAt());scored.add(new Scored(row,f,result));
            if(result.decision()==DecisionEngine.Outcome.APPROVE) approved.computeIfAbsent(p.customerId(),k->new ArrayList<>()).add(p);
        }
        return List.copyOf(scored);
    }
    private static boolean inWindow(Payment h,Payment p,long seconds) { return h.occurredAt().isAfter(p.occurredAt().minusSeconds(seconds)) && !h.occurredAt().isAfter(p.occurredAt()); }
    private static long count(List<Payment> history,Payment p,long seconds) { return history.stream().filter(h->inWindow(h,p,seconds)).count(); }
    private static double divide(long a,long b) { return b==0?0:(double)a/b; }
    public static Map<String,Object> metrics(List<Scored> rows,int review,int block,Costs costs) {
        long tp=0,fp=0,tn=0,fn=0,reviews=0,blocks=0,fraudCaptured=0,legitReview=0,legitBlock=0;
        for(Scored s:rows) {
            boolean positive=s.result().riskScore()>=review,fraud=s.row().fraud();long amount=s.row().payment().amountCents();
            if(positive && fraud) {tp++;fraudCaptured+=amount;}else if(positive)fp++;else if(fraud)fn++;else tn++;
            if(s.result().riskScore()>=block) {blocks++;if(!fraud)legitBlock+=amount;}
            else if(positive) {reviews++;if(!fraud)legitReview+=amount;}
        }
        double precision=divide(tp,tp+fp),recall=divide(tp,tp+fn);
        Map<String,Object> m=new LinkedHashMap<>();m.put("reviewThreshold",review);m.put("blockThreshold",block);m.put("count",rows.size());
        m.put("confusionMatrix",new TreeMap<>(Map.of("TP",tp,"FP",fp,"TN",tn,"FN",fn)));m.put("precision",precision);m.put("recall",recall);m.put("F1",precision+recall==0?0:2*precision*recall/(precision+recall));
        m.put("FPR",divide(fp,fp+tn));m.put("FNR",divide(fn,fn+tp));m.put("reviewRate",divide(reviews,rows.size()));m.put("blockRate",divide(blocks,rows.size()));m.put("approvalRate",divide(rows.size()-reviews-blocks,rows.size()));
        m.put("fraudAmountFlaggedCents",fraudCaptured);m.put("legitimateAmountReviewedCents",legitReview);m.put("legitimateAmountBlockedCents",legitBlock);m.put("demoCost",fp*costs.falsePositive()+fn*costs.falseNegative()+reviews*costs.review());return m;
    }
    public static void main(String[] args) throws Exception {
        long seed=args.length>0?Long.parseLong(args[0]):20261121L;Path out=Path.of(args.length>1?args[1]:"evaluation/results");
        Costs costs=new Costs(Long.getLong("demo.fp",10),Long.getLong("demo.fn",100),Long.getLong("demo.review",2));
        JsonMapper mapper=JsonMapper.builder().build();
        Ruleset config=args.length>2?mapper.readValue(Files.readString(Path.of(args[2])),Ruleset.class):Ruleset.baseline();
        List<Row> rows=generate(seed,200,40);List<Scored> scored=replay(rows,config);
        Instant split=START.plusSeconds(24*3600); // First 60% of base time used for calibration; future 40% held out.
        List<Scored> validation=scored.stream().filter(s->s.row().payment().occurredAt().isBefore(split)).toList(),test=scored.stream().filter(s->!s.row().payment().occurredAt().isBefore(split)).toList();
        List<Map<String,Object>> analysis=new ArrayList<>();
        for(int r:List.of(30,40,50,60)) for(int b:List.of(65,70,85)) if(r<b)analysis.add(metrics(validation,r,b,costs));
        Map<String,Object> selected=analysis.stream().min(Comparator.<Map<String,Object>>comparingLong(m->((Number)m.get("demoCost")).longValue()).thenComparingInt(m->((Number)m.get("reviewThreshold")).intValue()).thenComparingInt(m->((Number)m.get("blockThreshold")).intValue())).orElseThrow();
        Map<String,Object> report=new LinkedHashMap<>();report.put("datasetVersion","synthetic-1");report.put("generatorVersion","generator-1");report.put("seed",seed);report.put("size",rows.size());report.put("fraudPrevalence",divide(rows.stream().filter(Row::fraud).count(),rows.size()));
        Map<String,Long> distribution=new TreeMap<>();rows.forEach(r->distribution.merge(r.scenario(),1L,Long::sum));report.put("scenarioDistribution",distribution);
        report.put("ruleVersion",config.version());report.put("policyVersion",config.version());report.put("configuration",config);report.put("featureVersion","features-1");report.put("modelVersion","NONE");report.put("MLDecision","ML_NOT_INCLUDED_IN_V0.1.0");
        report.put("applicationCommit",System.getProperty("application.commit","UNCOMMITTED"));report.put("generatedAt",System.getProperty("evaluation.at",Instant.now().toString()));report.put("costs",costs);report.put("splitAt",split);report.put("thresholdAnalysisValidation",analysis);report.put("selectedByValidation",selected);
        report.put("baselineValidation",metrics(validation,config.reviewThreshold(),config.blockThreshold(),costs));report.put("baselineTest",metrics(test,config.reviewThreshold(),config.blockThreshold(),costs));
        report.put("positiveDefinition","REVIEW or BLOCK; flagging is not confirmed fraud");report.put("limitations","Synthetic labels; fixed-feature threshold comparison, no causal financial claim; PR/ROC AUC not computed; zero-denominator metrics reported as 0");
        Files.createDirectories(out);
        String dataset=rows.stream().map(mapper::writeValueAsString).collect(java.util.stream.Collectors.joining("\n","","\n"));
        Files.writeString(out.resolve("dataset.jsonl"),dataset);report.put("datasetSha256",java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(dataset.getBytes(StandardCharsets.UTF_8))));
        Files.writeString(out.resolve("evaluation.json"),mapper.writerWithDefaultPrettyPrinter().writeValueAsString(report));
        StringBuilder md=new StringBuilder("# Synthetic baseline evaluation\n\nSeed: "+seed+". Dataset: "+rows.size()+". No production fraud claims. Positive = REVIEW/BLOCK.\n\n## Held-out temporal test\n\n");
        metrics(test,config.reviewThreshold(),config.blockThreshold(),costs).forEach((k,v)->md.append("- ").append(k).append(": ").append(v).append('\n'));
        md.append("\n## Validation threshold analysis\n\n| Review | Block | Precision | Recall | FPR | Review rate | Demo cost |\n|---|---|---|---|---|---|---|\n");
        for(var m:analysis)md.append("| ").append(m.get("reviewThreshold")).append(" | ").append(m.get("blockThreshold")).append(" | ").append(m.get("precision")).append(" | ").append(m.get("recall")).append(" | ").append(m.get("FPR")).append(" | ").append(m.get("reviewRate")).append(" | ").append(m.get("demoCost")).append(" |\n");
        md.append("\nSelected by validation cost: ").append(selected.get("reviewThreshold")).append('/').append(selected.get("blockThreshold")).append(". Full metadata and scenario distribution in evaluation.json.\n\nML_NOT_INCLUDED_IN_V0.1.0\n");
        Files.writeString(out.resolve("evaluation.md"),md);System.out.println(mapper.writeValueAsString(report.get("baselineTest")));
    }
}
