-- Refactor job_offers: remove template/manager/body/pdf columns; rename join_date -> joining.
-- PostgreSQL: DROP COLUMN removes FK constraints on those columns.

ALTER TABLE job_offers
    DROP COLUMN IF EXISTS manager_id,
    DROP COLUMN IF EXISTS template_id,
    DROP COLUMN IF EXISTS body_html,
    DROP COLUMN IF EXISTS pdf_generated_at;

ALTER TABLE job_offers
    RENAME COLUMN joining to joining_date;
