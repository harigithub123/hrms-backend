-- Move offer lifecycle/email tracking out of job_offers into an audit/event table.
-- This keeps job_offers focused on current state, while preserving an append-only history.

CREATE TABLE IF NOT EXISTS job_offer_events (
    id                BIGSERIAL PRIMARY KEY,
    offer_id           BIGINT NOT NULL REFERENCES job_offers (id) ON DELETE CASCADE,
    action             VARCHAR(40) NOT NULL,
    action_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    action_by_user_id  BIGINT REFERENCES users (id),
    remark             VARCHAR(2000)
);

CREATE INDEX IF NOT EXISTS idx_job_offer_events_offer_id ON job_offer_events (offer_id);
CREATE INDEX IF NOT EXISTS idx_job_offer_events_action_at ON job_offer_events (action_at);
CREATE INDEX IF NOT EXISTS idx_job_offer_events_action ON job_offer_events (action);

-- Backfill existing lifecycle stamps (best-effort).
INSERT INTO job_offer_events (offer_id, action, action_at, action_by_user_id, remark)
SELECT id, 'RELEASED', sent_at, sent_by_user_id, NULL
FROM job_offers
WHERE sent_at IS NOT NULL;

INSERT INTO job_offer_events (offer_id, action, action_at, action_by_user_id, remark)
SELECT id, 'ACCEPTED', accepted_at, accepted_by_user_id, NULL
FROM job_offers
WHERE accepted_at IS NOT NULL;

INSERT INTO job_offer_events (offer_id, action, action_at, action_by_user_id, remark)
SELECT id, 'REJECTED', rejected_at, rejected_by_user_id, NULL
FROM job_offers
WHERE rejected_at IS NOT NULL;

INSERT INTO job_offer_events (offer_id, action, action_at, action_by_user_id, remark)
SELECT id, 'JOINED', joined_at, joined_by_user_id, NULL
FROM job_offers
WHERE joined_at IS NOT NULL;

INSERT INTO job_offer_events (offer_id, action, action_at, action_by_user_id, remark)
SELECT id,
       CASE WHEN last_email_status = 'SENT' THEN 'EMAIL_SENT' ELSE 'EMAIL_FAILED' END,
       COALESCE(sent_at, updated_at, created_at),
       sent_by_user_id,
       last_email_error
FROM job_offers
WHERE last_email_status IS NOT NULL AND last_email_status <> '';

-- Drop lifecycle/email fields from job_offers (now in job_offer_events).
ALTER TABLE job_offers
    DROP COLUMN IF EXISTS sent_at,
    DROP COLUMN IF EXISTS sent_by_user_id,
    DROP COLUMN IF EXISTS accepted_at,
    DROP COLUMN IF EXISTS accepted_by_user_id,
    DROP COLUMN IF EXISTS rejected_at,
    DROP COLUMN IF EXISTS rejected_by_user_id,
    DROP COLUMN IF EXISTS joined_at,
    DROP COLUMN IF EXISTS joined_by_user_id,
    DROP COLUMN IF EXISTS last_email_status,
    DROP COLUMN IF EXISTS last_email_error;

