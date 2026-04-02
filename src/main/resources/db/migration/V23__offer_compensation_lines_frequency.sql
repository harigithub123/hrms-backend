-- Add payout frequency per offer compensation line.
-- Needed so JOIN can create employee compensation lines with the same frequency.
ALTER TABLE job_offer_compensation_lines
    ADD COLUMN IF NOT EXISTS frequency VARCHAR(20) NOT NULL DEFAULT 'MONTHLY';

