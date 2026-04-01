-- Full removal of offer-level joining_bonus and yearly_bonus.
-- Bonuses are now represented as employee_compensation_lines with frequency/payable_on (Option A).

ALTER TABLE job_offers
    DROP COLUMN IF EXISTS joining_bonus,
    DROP COLUMN IF EXISTS yearly_bonus;

