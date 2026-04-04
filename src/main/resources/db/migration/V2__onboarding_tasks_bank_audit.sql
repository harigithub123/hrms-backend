-- Task workflow, comments, audit trail, and bank details per onboarding case

ALTER TABLE onboarding_tasks
    ADD COLUMN IF NOT EXISTS status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN IF NOT EXISTS comment_text VARCHAR(2000);

UPDATE onboarding_tasks SET status = 'DONE' WHERE done = true;

CREATE TABLE IF NOT EXISTS onboarding_task_audits (
    id                    BIGSERIAL PRIMARY KEY,
    task_id               BIGINT NOT NULL REFERENCES onboarding_tasks (id) ON DELETE CASCADE,
    action                VARCHAR(50) NOT NULL,
    detail                VARCHAR(2000),
    created_by_user_id    BIGINT REFERENCES users (id) ON DELETE SET NULL,
    created_by_username   VARCHAR(100),
    created_at            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_onboarding_task_audits_task ON onboarding_task_audits (task_id);
CREATE INDEX IF NOT EXISTS idx_onboarding_task_audits_created ON onboarding_task_audits (created_at DESC);

CREATE TABLE IF NOT EXISTS onboarding_bank_details (
    id                   BIGSERIAL PRIMARY KEY,
    case_id              BIGINT NOT NULL UNIQUE REFERENCES onboarding_cases (id) ON DELETE CASCADE,
    account_holder_name  VARCHAR(200) NOT NULL,
    bank_name            VARCHAR(200) NOT NULL,
    branch               VARCHAR(200),
    account_number       VARCHAR(80) NOT NULL,
    ifsc_code            VARCHAR(20) NOT NULL,
    account_type         VARCHAR(20) NOT NULL DEFAULT 'SAVINGS',
    notes                VARCHAR(500),
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
