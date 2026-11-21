import http from 'k6/http';
import { check, sleep } from 'k6';
import encoding from 'k6/encoding';
import { Counter } from 'k6/metrics';

const mode = __ENV.SCENARIO || 'steady';
const failures = new Counter('correctness_failures');
const distributions = new Counter('decision_distribution');
const degraded = new Counter('degraded_decisions');
const uuid = (run,vu,it) => `${run.slice(-8)}-${vu.toString(16).padStart(4,'0')}-4000-8000-${it.toString(16).padStart(12,'0')}`;
export function setup() { return {run:Date.now().toString(16).padStart(12,'0').slice(-12),time:new Date().toISOString()}; }
export const options = {
  vus: mode === 'concurrency' ? 12 : mode === 'burst' ? 8 : 4,
  duration: '15s',
  summaryTrendStats: ['avg','min','med','p(50)','p(95)','p(99)','max'],
  thresholds: { http_req_failed: ['rate==0'], checks: ['rate==1'], correctness_failures: ['count==0'] },
};
export default function (data) {
  const t = mode === 'duplicate' ? data.time : new Date().toISOString();
  const id = mode === 'duplicate' ? `00000000-0000-4000-8000-${data.run}` : uuid(data.run,__VU,__ITER);
  const customer = mode==='duplicate'?'syn_load_duplicate':mode==='velocity'?'syn_load_hot':`syn_load_${__VU}`;
  const device = mode==='duplicate'?'syn_load_duplicate':mode==='velocity'?'syn_load_shared':`syn_load_d${__VU}`;
  const body = JSON.stringify({ transactionId:id, customerId:customer, accountId:'syn_load',amountCents:1000,currency:'BRL',merchantId:'syn_load',deviceId:device,country:'BR',occurredAt:t,channel:'WEB' });
  const params = {headers:{'Content-Type':'application/json',Authorization:'Basic '+encoding.b64encode('engine:'+__ENV.ENGINE_PASSWORD)}};
  const res = http.post(`${__ENV.BASE_URL}/risk/evaluations`,body,params);
  let doc;
  try { doc=res.json(); } catch (_) { failures.add(1);return; }
  const valid=check(res,{'durable 200':r=>r.status===200,'bounded score':()=>doc.result && doc.result.riskScore>=0 && doc.result.riskScore<=100,'audit identity':()=>doc.transactionId===id && !!doc.decisionId,'valid outcome':()=>doc.result && ['APPROVE','REVIEW','BLOCK'].includes(doc.result.decision)});
  if(!valid) failures.add(1); else { distributions.add(1,{decision:doc.result.decision});degraded.add(doc.result.degraded?1:0); }
  if(mode==='duplicate') {
    const again=http.post(`${__ENV.BASE_URL}/risk/evaluations`,body,params);
    if(!check(again,{'duplicate same audit':r=>r.status===200 && r.json().decisionId===doc.decisionId})) failures.add(1);
  }
  sleep(mode==='steady'?0.1:0.01);
}
