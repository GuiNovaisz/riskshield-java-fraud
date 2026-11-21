-- transaction_id PK already guarantees uniqueness. A redundant composite unique index can
-- race with the ON CONFLICT(transaction_id) arbiter during simultaneous speculative inserts.
ALTER TABLE intake DROP CONSTRAINT intake_transaction_id_payload_hash_key;
