CREATE TABLE rulesets (
  version varchar(48) PRIMARY KEY,
  definition jsonb NOT NULL,
  maker varchar(80) NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE config_state (
  version varchar(48) PRIMARY KEY REFERENCES rulesets,
  state varchar(16) NOT NULL CHECK (state IN ('DRAFT','APPROVED','SHADOW','ACTIVE','RETIRED')),
  checker varchar(80),
  CHECK (state = 'DRAFT' OR checker IS NOT NULL)
);
CREATE UNIQUE INDEX one_active_config ON config_state(state) WHERE state = 'ACTIVE';
CREATE UNIQUE INDEX one_shadow_config ON config_state(state) WHERE state = 'SHADOW';
CREATE TABLE intake (
  transaction_id uuid PRIMARY KEY,
  payload_hash char(64) NOT NULL,
  payment jsonb NOT NULL,
  customer_id varchar(68) NOT NULL,
  device_id varchar(68) NOT NULL,
  merchant_id varchar(68) NOT NULL,
  country char(2) NOT NULL,
  amount_cents bigint NOT NULL CHECK (amount_cents > 0 AND amount_cents <= 10000000000),
  occurred_at timestamptz NOT NULL,
  received_at timestamptz NOT NULL,
  UNIQUE(transaction_id,payload_hash)
);
CREATE INDEX intake_customer_time ON intake(customer_id,occurred_at DESC);
CREATE INDEX intake_device_time ON intake(device_id,occurred_at DESC);
CREATE TABLE decisions (
  decision_id uuid PRIMARY KEY,
  transaction_id uuid NOT NULL UNIQUE REFERENCES intake,
  document jsonb NOT NULL,
  outcome varchar(8) NOT NULL CHECK (outcome IN ('APPROVE','REVIEW','BLOCK')),
  score integer NOT NULL CHECK (score BETWEEN 0 AND 100),
  rule_version varchar(48) NOT NULL REFERENCES rulesets,
  policy_version varchar(48) NOT NULL REFERENCES rulesets,
  evaluated_at timestamptz NOT NULL
);
CREATE INDEX review_queue ON decisions(evaluated_at DESC,decision_id) WHERE outcome='REVIEW';
CREATE TABLE feedback (
  feedback_id uuid PRIMARY KEY,
  decision_id uuid NOT NULL REFERENCES decisions,
  label varchar(16) NOT NULL CHECK (label IN ('FRAUD','LEGITIMATE','UNKNOWN')),
  outcome varchar(24) NOT NULL CHECK (outcome IN ('CONFIRMED_FRAUD','LEGITIMATE','INCONCLUSIVE')),
  actor varchar(80) NOT NULL,
  reason varchar(240) NOT NULL,
  observed_at timestamptz NOT NULL,
  recorded_at timestamptz NOT NULL DEFAULT now(),
  CHECK ((label='FRAUD' AND outcome='CONFIRMED_FRAUD') OR (label='LEGITIMATE' AND outcome='LEGITIMATE') OR (label='UNKNOWN' AND outcome='INCONCLUSIVE'))
);
CREATE INDEX feedback_decision ON feedback(decision_id,recorded_at DESC);
CREATE TABLE shadow_comparisons (
  decision_id uuid NOT NULL REFERENCES decisions,
  candidate_version varchar(48) NOT NULL REFERENCES rulesets,
  document jsonb NOT NULL,
  disagreed boolean NOT NULL,
  PRIMARY KEY(decision_id,candidate_version)
);
CREATE TABLE admin_audit (
  audit_id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  actor varchar(80) NOT NULL,
  action varchar(32) NOT NULL,
  version varchar(48) NOT NULL REFERENCES rulesets,
  before_state varchar(16),
  after_state varchar(16) NOT NULL,
  reason varchar(240) NOT NULL,
  occurred_at timestamptz NOT NULL
);
CREATE TABLE evaluation_runs (
  run_id uuid PRIMARY KEY,
  manifest jsonb NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now()
);
CREATE FUNCTION reject_mutation() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN RAISE EXCEPTION 'append-only audit: mutation forbidden' USING ERRCODE='23514'; END $$;
CREATE TRIGGER immutable_intake BEFORE UPDATE OR DELETE ON intake FOR EACH ROW EXECUTE FUNCTION reject_mutation();
CREATE TRIGGER immutable_decision BEFORE UPDATE OR DELETE ON decisions FOR EACH ROW EXECUTE FUNCTION reject_mutation();
CREATE TRIGGER immutable_ruleset BEFORE UPDATE OR DELETE ON rulesets FOR EACH ROW EXECUTE FUNCTION reject_mutation();
CREATE TRIGGER immutable_feedback BEFORE UPDATE OR DELETE ON feedback FOR EACH ROW EXECUTE FUNCTION reject_mutation();
CREATE TRIGGER immutable_admin_audit BEFORE UPDATE OR DELETE ON admin_audit FOR EACH ROW EXECUTE FUNCTION reject_mutation();
CREATE TRIGGER immutable_shadow BEFORE UPDATE OR DELETE ON shadow_comparisons FOR EACH ROW EXECUTE FUNCTION reject_mutation();
CREATE TRIGGER immutable_run BEFORE UPDATE OR DELETE ON evaluation_runs FOR EACH ROW EXECUTE FUNCTION reject_mutation();
