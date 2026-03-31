-- Offers enhancements: employee type, extra fields, audit stamps, email status, join -> employee link,
-- plus offer-specific compensation lines referencing salary_components.
-- Also extend employees with mobile_number (needed for joined onboarding).

-- Employees: add mobile number
ALTER TABLE employees
    ADD COLUMN IF NOT EXISTS mobile_number VARCHAR(30);

-- Job offers: enrich offer inputs and lifecycle/audit fields
ALTER TABLE job_offers
    ADD COLUMN IF NOT EXISTS employee_type VARCHAR(30),
    ADD COLUMN IF NOT EXISTS offer_release_date DATE,
    ADD COLUMN IF NOT EXISTS probation_period_months INT,
    ADD COLUMN IF NOT EXISTS joining_bonus NUMERIC(14, 2),
    ADD COLUMN IF NOT EXISTS yearly_bonus NUMERIC(14, 2),
    ADD COLUMN IF NOT EXISTS candidate_mobile VARCHAR(30),
    ADD COLUMN IF NOT EXISTS sent_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS sent_by_user_id BIGINT REFERENCES users (id),
    ADD COLUMN IF NOT EXISTS accepted_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS accepted_by_user_id BIGINT REFERENCES users (id),
    ADD COLUMN IF NOT EXISTS rejected_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS rejected_by_user_id BIGINT REFERENCES users (id),
    ADD COLUMN IF NOT EXISTS joined_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS joined_by_user_id BIGINT REFERENCES users (id),
    ADD COLUMN IF NOT EXISTS last_email_status VARCHAR(20),
    ADD COLUMN IF NOT EXISTS last_email_error VARCHAR(2000),
    ADD COLUMN IF NOT EXISTS employee_id BIGINT REFERENCES employees (id);

CREATE INDEX IF NOT EXISTS idx_job_offers_employee_type ON job_offers (employee_type);
CREATE INDEX IF NOT EXISTS idx_job_offers_employee_id ON job_offers (employee_id);

-- Offer compensation (one per offer; optional)
CREATE TABLE IF NOT EXISTS job_offer_compensation (
    id          BIGSERIAL PRIMARY KEY,
    offer_id    BIGINT NOT NULL REFERENCES job_offers (id) ON DELETE CASCADE,
    currency    VARCHAR(10) NOT NULL DEFAULT 'INR',
    annual_ctc  NUMERIC(14, 2),
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (offer_id)
);

CREATE TABLE IF NOT EXISTS job_offer_compensation_lines (
    id                     BIGSERIAL PRIMARY KEY,
    offer_compensation_id  BIGINT NOT NULL REFERENCES job_offer_compensation (id) ON DELETE CASCADE,
    component_id           BIGINT NOT NULL REFERENCES salary_components (id),
    amount                 NUMERIC(14, 2) NOT NULL,
    UNIQUE (offer_compensation_id, component_id)
);

CREATE INDEX IF NOT EXISTS idx_offer_comp_lines_comp ON job_offer_compensation_lines (offer_compensation_id);

