-- Compensation (authoring layer; sync to salary structure optional from app)
CREATE TABLE employee_compensation (
    id              BIGSERIAL PRIMARY KEY,
    employee_id     BIGINT NOT NULL REFERENCES employees (id) ON DELETE CASCADE,
    effective_from  DATE   NOT NULL,
    effective_to    DATE,
    currency        VARCHAR(10) NOT NULL DEFAULT 'INR',
    annual_ctc      NUMERIC(14, 2),
    notes           VARCHAR(2000),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_emp_comp_employee_effective ON employee_compensation (employee_id, effective_from DESC);

CREATE TABLE employee_compensation_lines (
    id               BIGSERIAL PRIMARY KEY,
    compensation_id  BIGINT NOT NULL REFERENCES employee_compensation (id) ON DELETE CASCADE,
    component_id     BIGINT NOT NULL REFERENCES salary_components (id),
    amount           NUMERIC(14, 2) NOT NULL,
    UNIQUE (compensation_id, component_id)
);

-- Offer letters
CREATE TABLE offer_templates (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(200) NOT NULL,
    body_html   TEXT         NOT NULL,
    active      BOOLEAN      NOT NULL DEFAULT true,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE job_offers (
    id               BIGSERIAL PRIMARY KEY,
    template_id      BIGINT REFERENCES offer_templates (id) ON DELETE SET NULL,
    candidate_name   VARCHAR(200) NOT NULL,
    candidate_email  VARCHAR(255),
    status           VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    department_id    BIGINT REFERENCES departments (id),
    designation_id   BIGINT REFERENCES designations (id),
    manager_id       BIGINT REFERENCES employees (id),
    join_date        DATE,
    annual_ctc       NUMERIC(14, 2),
    currency         VARCHAR(10) NOT NULL DEFAULT 'INR',
    body_html        TEXT,
    pdf_generated_at TIMESTAMP WITH TIME ZONE,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_job_offers_status ON job_offers (status);

-- Onboarding (may reference an accepted offer)
CREATE TABLE onboarding_cases (
    id                   BIGSERIAL PRIMARY KEY,
    status               VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    candidate_first_name VARCHAR(100) NOT NULL,
    candidate_last_name  VARCHAR(100) NOT NULL,
    candidate_email      VARCHAR(255),
    join_date            DATE   NOT NULL,
    department_id        BIGINT REFERENCES departments (id),
    designation_id       BIGINT REFERENCES designations (id),
    manager_id           BIGINT REFERENCES employees (id),
    employee_id          BIGINT REFERENCES employees (id),
    offer_id             BIGINT REFERENCES job_offers (id),
    assigned_hr_user_id  BIGINT REFERENCES users (id),
    notes                VARCHAR(2000),
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_onboarding_status ON onboarding_cases (status);

CREATE TABLE onboarding_tasks (
    id          BIGSERIAL PRIMARY KEY,
    case_id     BIGINT NOT NULL REFERENCES onboarding_cases (id) ON DELETE CASCADE,
    label       VARCHAR(300) NOT NULL,
    done        BOOLEAN NOT NULL DEFAULT false,
    sort_order  INT NOT NULL DEFAULT 0
);

-- Salary advances
CREATE TABLE salary_advances (
    id                         BIGSERIAL PRIMARY KEY,
    employee_id                BIGINT NOT NULL REFERENCES employees (id) ON DELETE CASCADE,
    amount                     NUMERIC(14, 2) NOT NULL,
    currency                   VARCHAR(10) NOT NULL DEFAULT 'INR',
    reason                     VARCHAR(2000),
    status                     VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    requested_at               TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    approved_by_user_id        BIGINT REFERENCES users (id),
    approved_at                TIMESTAMP WITH TIME ZONE,
    rejected_reason            VARCHAR(1000),
    payout_date                DATE,
    paid_at                    TIMESTAMP WITH TIME ZONE,
    recovery_months            INT NOT NULL DEFAULT 1,
    recovery_amount_per_month  NUMERIC(14, 2),
    outstanding_balance        NUMERIC(14, 2),
    created_at                 TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                 TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_salary_advances_employee ON salary_advances (employee_id);
CREATE INDEX idx_salary_advances_status ON salary_advances (status);

CREATE TABLE payslip_advance_deductions (
    id          BIGSERIAL PRIMARY KEY,
    payslip_id  BIGINT NOT NULL REFERENCES payslips (id) ON DELETE CASCADE,
    advance_id  BIGINT NOT NULL REFERENCES salary_advances (id),
    amount      NUMERIC(14, 2) NOT NULL,
    UNIQUE (payslip_id, advance_id)
);

CREATE INDEX idx_payslip_adv_ded_payslip ON payslip_advance_deductions (payslip_id);

INSERT INTO salary_components (code, name, kind, sort_order)
SELECT 'ADVANCE_RECOVERY', 'Salary advance recovery', 'DEDUCTION', 200
WHERE NOT EXISTS (SELECT 1 FROM salary_components WHERE code = 'ADVANCE_RECOVERY');

INSERT INTO offer_templates (name, body_html, active)
SELECT 'Standard offer',
       '<p>Dear {{candidateName}},</p><p>We are pleased to offer you the position of <strong>{{designation}}</strong> in <strong>{{department}}</strong>, reporting to {{managerName}}.</p><p>Proposed start date: {{joinDate}}. Annual CTC: {{currency}} {{annualCtc}}.</p><p>Regards,<br/>HR</p>',
       true
WHERE NOT EXISTS (SELECT 1 FROM offer_templates WHERE name = 'Standard offer');
