-- Option A: model payout frequency on each compensation line.
-- Also remove header-level joining_bonus/yearly_bonus and backfill them into lines.

ALTER TABLE employee_compensation_lines
    ADD COLUMN IF NOT EXISTS frequency VARCHAR(20) NOT NULL DEFAULT 'MONTHLY',
    ADD COLUMN IF NOT EXISTS payable_on DATE;

-- Ensure salary components exist for bonus lines (needed for backfill too).
INSERT INTO salary_components (code, name, kind, sort_order)
SELECT 'JOINING_BONUS', 'Joining bonus', 'EARNING', 150
WHERE NOT EXISTS (SELECT 1 FROM salary_components WHERE code = 'JOINING_BONUS');

INSERT INTO salary_components (code, name, kind, sort_order)
SELECT 'ANNUAL_BONUS', 'Annual bonus', 'EARNING', 160
WHERE NOT EXISTS (SELECT 1 FROM salary_components WHERE code = 'ANNUAL_BONUS');

-- Backfill existing header-level bonus columns into lines (if those columns exist).
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name='employee_compensation' AND column_name='joining_bonus'
    ) THEN
        INSERT INTO employee_compensation_lines (compensation_id, component_id, amount, frequency, payable_on)
        SELECT c.id, sc.id, c.joining_bonus, 'ONE_TIME', c.effective_from
        FROM employee_compensation c
        JOIN salary_components sc ON sc.code = 'JOINING_BONUS'
        WHERE c.joining_bonus IS NOT NULL AND c.joining_bonus <> 0
        ON CONFLICT DO NOTHING;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name='employee_compensation' AND column_name='yearly_bonus'
    ) THEN
        INSERT INTO employee_compensation_lines (compensation_id, component_id, amount, frequency, payable_on)
        SELECT c.id, sc.id, c.yearly_bonus, 'YEARLY', NULL
        FROM employee_compensation c
        JOIN salary_components sc ON sc.code = 'ANNUAL_BONUS'
        WHERE c.yearly_bonus IS NOT NULL AND c.yearly_bonus <> 0
        ON CONFLICT DO NOTHING;
    END IF;
END $$;

-- Drop header bonus columns.
ALTER TABLE employee_compensation
    DROP COLUMN IF EXISTS joining_bonus,
    DROP COLUMN IF EXISTS yearly_bonus;

-- Remove old unique constraint so the same component can exist with different frequency/payout dates if needed.
ALTER TABLE employee_compensation_lines
    DROP CONSTRAINT IF EXISTS employee_compensation_lines_compensation_id_component_id_key;

CREATE UNIQUE INDEX IF NOT EXISTS uq_emp_comp_lines_comp_component_freq_payable
    ON employee_compensation_lines (compensation_id, component_id, frequency, COALESCE(payable_on, DATE '1900-01-01'));

