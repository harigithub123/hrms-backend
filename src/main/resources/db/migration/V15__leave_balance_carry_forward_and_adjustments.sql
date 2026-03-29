-- Carry-forward policy limits (per leave type)
ALTER TABLE leave_types
    ADD COLUMN IF NOT EXISTS max_carry_forward_per_year NUMERIC(7, 2),
    ADD COLUMN IF NOT EXISTS max_carry_forward NUMERIC(7, 2);

COMMENT ON COLUMN leave_types.max_carry_forward_per_year IS 'Max days that may be carried from one year into the next (per rollover). NULL = no limit.';
COMMENT ON COLUMN leave_types.max_carry_forward IS 'Max total carried-forward balance allowed. NULL = no limit.';

-- Balance: portion brought from prior years (manual or rollover)
ALTER TABLE leave_balances
    ADD COLUMN IF NOT EXISTS carry_forwarded_days NUMERIC(7, 2) NOT NULL DEFAULT 0;

-- HR adjustments with comments (allocation changes and carry-forward changes)
CREATE TABLE leave_balance_adjustments (
    id                   BIGSERIAL PRIMARY KEY,
    leave_balance_id     BIGINT        NOT NULL REFERENCES leave_balances (id) ON DELETE CASCADE,
    adjustment_kind      VARCHAR(30)   NOT NULL,
    delta_days           NUMERIC(7, 2) NOT NULL,
    comment_text         VARCHAR(2000) NOT NULL,
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by_user_id   BIGINT REFERENCES users (id) ON DELETE SET NULL
);

CREATE INDEX idx_leave_balance_adjustments_balance ON leave_balance_adjustments (leave_balance_id);
CREATE INDEX idx_leave_balance_adjustments_created ON leave_balance_adjustments (created_at DESC);
