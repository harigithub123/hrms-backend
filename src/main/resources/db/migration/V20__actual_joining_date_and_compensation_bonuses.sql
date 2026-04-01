-- Capture actual joining date for offers and store one-time bonuses on employee compensation.

ALTER TABLE job_offers
    ADD COLUMN IF NOT EXISTS actual_joining_date DATE;

ALTER TABLE employee_compensation
    ADD COLUMN IF NOT EXISTS joining_bonus NUMERIC(14, 2),
    ADD COLUMN IF NOT EXISTS yearly_bonus NUMERIC(14, 2);

