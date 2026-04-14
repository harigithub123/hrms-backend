-- Remove salary component code column and all currency columns (hardcoded in app),
-- and extend salary structures with effective_to + is_active.
--
-- Postgres dialect (Flyway configured with flyway-database-postgresql).

-- ---------------------------------------------------------------------------
-- salary_components: drop code
-- ---------------------------------------------------------------------------
ALTER TABLE salary_components
    DROP CONSTRAINT IF EXISTS salary_components_code_key;

ALTER TABLE salary_components
    DROP COLUMN IF EXISTS code;

-- ---------------------------------------------------------------------------
-- employee_salary_structures: add effective_to + is_active, drop currency
-- ---------------------------------------------------------------------------
ALTER TABLE employee_salary_structures
    ADD COLUMN IF NOT EXISTS effective_to DATE;

ALTER TABLE employee_salary_structures
    ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT true;

ALTER TABLE employee_salary_structures
    DROP COLUMN IF EXISTS currency;

-- ---------------------------------------------------------------------------
-- employee_compensation: drop currency
-- ---------------------------------------------------------------------------
ALTER TABLE employee_compensation
    DROP COLUMN IF EXISTS currency;

-- ---------------------------------------------------------------------------
-- salary_advances: drop currency
-- ---------------------------------------------------------------------------
ALTER TABLE salary_advances
    DROP COLUMN IF EXISTS currency;

-- ---------------------------------------------------------------------------
-- job offers / offer compensation: drop currency
-- ---------------------------------------------------------------------------
ALTER TABLE job_offers
    DROP COLUMN IF EXISTS currency;

ALTER TABLE job_offer_compensation
    DROP COLUMN IF EXISTS currency;

-- ---------------------------------------------------------------------------
-- employee_compensation_lines: drop payable_on
-- ---------------------------------------------------------------------------
ALTER TABLE employee_compensation_lines
    DROP COLUMN IF EXISTS payable_on;

