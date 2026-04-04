-- Consolidated HRMS schema (replaces former V1–V23 chain). Intended for new databases.
-- If you already applied older migrations, do not replace them; Flyway checksums will conflict.

-- ---------------------------------------------------------------------------
-- Roles & org (employees before users because of users.employee_id FK)
-- ---------------------------------------------------------------------------

CREATE TABLE roles (
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE departments (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(150) NOT NULL,
    code        VARCHAR(50),
    description VARCHAR(500),
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX idx_departments_code ON departments (code) WHERE code IS NOT NULL;

CREATE TABLE designations (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(150) NOT NULL,
    code        VARCHAR(50),
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX idx_designations_code ON designations (code) WHERE code IS NOT NULL;

CREATE TABLE employees (
    id              BIGSERIAL PRIMARY KEY,
    employee_code   VARCHAR(50),
    first_name      VARCHAR(100) NOT NULL,
    last_name       VARCHAR(100) NOT NULL,
    email           VARCHAR(255),
    mobile_number   VARCHAR(30),
    department_id   BIGINT REFERENCES departments (id) ON DELETE SET NULL,
    designation_id  BIGINT REFERENCES designations (id) ON DELETE SET NULL,
    manager_id      BIGINT REFERENCES employees (id) ON DELETE SET NULL,
    joined_at       DATE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_employees_department ON employees (department_id);
CREATE INDEX idx_employees_designation ON employees (designation_id);
CREATE INDEX idx_employees_manager ON employees (manager_id);
CREATE UNIQUE INDEX idx_employees_code ON employees (employee_code) WHERE employee_code IS NOT NULL;

-- ---------------------------------------------------------------------------
-- Users & sessions
-- ---------------------------------------------------------------------------

CREATE TABLE users (
    id           BIGSERIAL PRIMARY KEY,
    username     VARCHAR(100) NOT NULL UNIQUE,
    password     VARCHAR(255)   NOT NULL,
    email        VARCHAR(255),
    enabled      BOOLEAN        NOT NULL DEFAULT true,
    employee_id  BIGINT REFERENCES employees (id) ON DELETE SET NULL,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_employee_id ON users (employee_id);
CREATE UNIQUE INDEX uq_users_employee_id ON users (employee_id) WHERE employee_id IS NOT NULL;

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES roles (id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

CREATE INDEX idx_user_roles_user_id ON user_roles (user_id);
CREATE INDEX idx_user_roles_role_id ON user_roles (role_id);

CREATE TABLE refresh_tokens (
    id         BIGSERIAL PRIMARY KEY,
    token      VARCHAR(500) NOT NULL UNIQUE,
    user_id    BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked    BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_refresh_tokens_token ON refresh_tokens (token);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);

-- ---------------------------------------------------------------------------
-- Leave & attendance
-- ---------------------------------------------------------------------------

CREATE TABLE leave_types (
    id                          BIGSERIAL PRIMARY KEY,
    name                        VARCHAR(150) NOT NULL,
    code                        VARCHAR(50)  NOT NULL UNIQUE,
    days_per_year               NUMERIC(7, 2) NOT NULL DEFAULT 0,
    carry_forward               BOOLEAN      NOT NULL DEFAULT false,
    paid                        BOOLEAN      NOT NULL DEFAULT true,
    active                      BOOLEAN      NOT NULL DEFAULT true,
    max_carry_forward_per_year  NUMERIC(7, 2),
    max_carry_forward           NUMERIC(7, 2),
    created_at                  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON COLUMN leave_types.max_carry_forward_per_year IS 'Max days that may be carried from one year into the next (per rollover). NULL = no limit.';
COMMENT ON COLUMN leave_types.max_carry_forward IS 'Max total carried-forward balance allowed. NULL = no limit.';

CREATE TABLE leave_balances (
    id                   BIGSERIAL PRIMARY KEY,
    employee_id          BIGINT NOT NULL REFERENCES employees (id) ON DELETE CASCADE,
    leave_type_id        BIGINT NOT NULL REFERENCES leave_types (id) ON DELETE CASCADE,
    year                 INT    NOT NULL,
    allocated_days       NUMERIC(7, 2) NOT NULL DEFAULT 0,
    used_days            NUMERIC(7, 2) NOT NULL DEFAULT 0,
    carry_forwarded_days NUMERIC(7, 2) NOT NULL DEFAULT 0,
    UNIQUE (employee_id, leave_type_id, year)
);
CREATE INDEX idx_leave_balances_employee ON leave_balances (employee_id);

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

CREATE TABLE leave_requests (
    id                  BIGSERIAL PRIMARY KEY,
    employee_id         BIGINT NOT NULL REFERENCES employees (id) ON DELETE CASCADE,
    leave_type_id       BIGINT NOT NULL REFERENCES leave_types (id),
    start_date          DATE   NOT NULL,
    end_date            DATE   NOT NULL,
    total_days          NUMERIC(7, 2) NOT NULL,
    reason              VARCHAR(2000),
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    requested_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    decided_at          TIMESTAMP WITH TIME ZONE,
    decided_by_user_id  BIGINT REFERENCES users (id) ON DELETE SET NULL,
    decision_comment    VARCHAR(500)
);
CREATE INDEX idx_leave_requests_employee ON leave_requests (employee_id);
CREATE INDEX idx_leave_requests_status ON leave_requests (status);
CREATE INDEX idx_leave_requests_range ON leave_requests (start_date, end_date);

CREATE TABLE attendance_records (
    id           BIGSERIAL PRIMARY KEY,
    employee_id  BIGINT NOT NULL REFERENCES employees (id) ON DELETE CASCADE,
    work_date    DATE   NOT NULL,
    check_in     TIME,
    check_out    TIME,
    status       VARCHAR(20) NOT NULL DEFAULT 'PRESENT',
    notes        VARCHAR(500),
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (employee_id, work_date)
);
CREATE INDEX idx_attendance_employee_date ON attendance_records (employee_id, work_date);

-- ---------------------------------------------------------------------------
-- Payroll components & payslips
-- ---------------------------------------------------------------------------

CREATE TABLE salary_components (
    id         BIGSERIAL PRIMARY KEY,
    code       VARCHAR(50)  NOT NULL UNIQUE,
    name       VARCHAR(150) NOT NULL,
    kind       VARCHAR(20)  NOT NULL,
    sort_order INT          NOT NULL DEFAULT 0,
    active     BOOLEAN      NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE employee_salary_structures (
    id             BIGSERIAL PRIMARY KEY,
    employee_id    BIGINT NOT NULL REFERENCES employees (id) ON DELETE CASCADE,
    effective_from DATE   NOT NULL,
    currency       VARCHAR(10) NOT NULL DEFAULT 'INR',
    note           VARCHAR(500),
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_salary_struct_emp_effective ON employee_salary_structures (employee_id, effective_from DESC);

CREATE TABLE employee_salary_structure_lines (
    id            BIGSERIAL PRIMARY KEY,
    structure_id  BIGINT NOT NULL REFERENCES employee_salary_structures (id) ON DELETE CASCADE,
    component_id  BIGINT NOT NULL REFERENCES salary_components (id),
    amount        NUMERIC(14, 2) NOT NULL,
    UNIQUE (structure_id, component_id)
);

CREATE TABLE pay_runs (
    id            BIGSERIAL PRIMARY KEY,
    period_start  DATE   NOT NULL,
    period_end    DATE   NOT NULL,
    status        VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE payslips (
    id                BIGSERIAL PRIMARY KEY,
    pay_run_id        BIGINT NOT NULL REFERENCES pay_runs (id) ON DELETE CASCADE,
    employee_id       BIGINT NOT NULL REFERENCES employees (id) ON DELETE CASCADE,
    gross_amount      NUMERIC(14, 2) NOT NULL,
    deduction_amount  NUMERIC(14, 2) NOT NULL,
    net_amount        NUMERIC(14, 2) NOT NULL,
    pdf_generated_at  TIMESTAMP WITH TIME ZONE,
    UNIQUE (pay_run_id, employee_id)
);
CREATE INDEX idx_payslips_run ON payslips (pay_run_id);

CREATE TABLE payslip_lines (
    id              BIGSERIAL PRIMARY KEY,
    payslip_id      BIGINT NOT NULL REFERENCES payslips (id) ON DELETE CASCADE,
    component_id    BIGINT REFERENCES salary_components (id) ON DELETE SET NULL,
    component_code  VARCHAR(50)  NOT NULL,
    component_name  VARCHAR(150) NOT NULL,
    kind            VARCHAR(20)  NOT NULL,
    amount          NUMERIC(14, 2) NOT NULL
);
CREATE INDEX idx_payslip_lines_payslip ON payslip_lines (payslip_id);

-- ---------------------------------------------------------------------------
-- Holidays
-- ---------------------------------------------------------------------------

CREATE TABLE holidays (
    id            BIGSERIAL PRIMARY KEY,
    holiday_date  DATE         NOT NULL UNIQUE,
    name          VARCHAR(200) NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ---------------------------------------------------------------------------
-- Employee compensation (bonuses live on lines with frequency / payable_on)
-- ---------------------------------------------------------------------------

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
    frequency        VARCHAR(20) NOT NULL DEFAULT 'MONTHLY',
    payable_on       DATE
);
CREATE UNIQUE INDEX uq_emp_comp_lines_comp_component_freq_payable
    ON employee_compensation_lines (compensation_id, component_id, frequency, COALESCE(payable_on, DATE '1900-01-01'));

-- ---------------------------------------------------------------------------
-- Offers & onboarding
-- ---------------------------------------------------------------------------

CREATE TABLE offer_templates (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(200) NOT NULL,
    body_html   TEXT         NOT NULL,
    active      BOOLEAN      NOT NULL DEFAULT true,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE job_offers (
    id                      BIGSERIAL PRIMARY KEY,
    candidate_name          VARCHAR(200) NOT NULL,
    candidate_email         VARCHAR(255),
    candidate_mobile        VARCHAR(30),
    status                  VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    employee_type           VARCHAR(30),
    department_id           BIGINT REFERENCES departments (id),
    designation_id          BIGINT REFERENCES designations (id),
    joining_date            DATE,
    offer_release_date      DATE,
    actual_joining_date     DATE,
    probation_period_months INT,
    annual_ctc              NUMERIC(14, 2),
    currency                VARCHAR(10) NOT NULL DEFAULT 'INR',
    employee_id             BIGINT REFERENCES employees (id),
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_job_offers_status ON job_offers (status);
CREATE INDEX idx_job_offers_employee_type ON job_offers (employee_type);
CREATE INDEX idx_job_offers_employee_id ON job_offers (employee_id);

CREATE TABLE job_offer_compensation (
    id          BIGSERIAL PRIMARY KEY,
    offer_id    BIGINT NOT NULL REFERENCES job_offers (id) ON DELETE CASCADE,
    currency    VARCHAR(10) NOT NULL DEFAULT 'INR',
    annual_ctc  NUMERIC(14, 2),
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (offer_id)
);

CREATE TABLE job_offer_compensation_lines (
    id                     BIGSERIAL PRIMARY KEY,
    offer_compensation_id  BIGINT NOT NULL REFERENCES job_offer_compensation (id) ON DELETE CASCADE,
    component_id           BIGINT NOT NULL REFERENCES salary_components (id),
    amount                 NUMERIC(14, 2) NOT NULL,
    frequency              VARCHAR(20) NOT NULL DEFAULT 'MONTHLY',
    UNIQUE (offer_compensation_id, component_id)
);
CREATE INDEX idx_offer_comp_lines_comp ON job_offer_compensation_lines (offer_compensation_id);

CREATE TABLE job_offer_events (
    id                  BIGSERIAL PRIMARY KEY,
    offer_id            BIGINT NOT NULL REFERENCES job_offers (id) ON DELETE CASCADE,
    action              VARCHAR(40) NOT NULL,
    action_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    action_by_user_id   BIGINT REFERENCES users (id),
    remark              VARCHAR(2000)
);
CREATE INDEX idx_job_offer_events_offer_id ON job_offer_events (offer_id);
CREATE INDEX idx_job_offer_events_action_at ON job_offer_events (action_at);
CREATE INDEX idx_job_offer_events_action ON job_offer_events (action);

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

-- ---------------------------------------------------------------------------
-- Seed data
-- ---------------------------------------------------------------------------

INSERT INTO roles (name)
VALUES ('ROLE_ADMIN'),
       ('ROLE_HR'),
       ('ROLE_USER'),
       ('ROLE_EMPLOYEE');

-- password = "password" (BCrypt strength 10)
INSERT INTO users (username, password, email, enabled, employee_id)
VALUES ('admin', '$2a$10$vd0pjEdj7QF9BwnakDcJJue.TUJM6Zk/t3yUqsI05btOQ297PoTCi', 'admin@hrms.local', true, NULL);

INSERT INTO user_roles (user_id, role_id)
VALUES (1, (SELECT id FROM roles WHERE name = 'ROLE_ADMIN')),
       (1, (SELECT id FROM roles WHERE name = 'ROLE_HR')),
       (1, (SELECT id FROM roles WHERE name = 'ROLE_USER'));

INSERT INTO departments (name, code) VALUES ('Engineering', 'ENG');

INSERT INTO designations (name, code)
VALUES ('Engineering Manager', 'MGR'),
       ('Software Engineer', 'SE');

INSERT INTO employees (employee_code, first_name, last_name, email, department_id, designation_id, manager_id)
SELECT 'EMP-ANKIT', 'Ankit', 'Kumar', 'ankit@demo.local', d.id, des.id, NULL
FROM departments d, designations des
WHERE d.code = 'ENG' AND des.code = 'MGR';

INSERT INTO employees (employee_code, first_name, last_name, email, department_id, designation_id, manager_id)
SELECT 'EMP-HARI', 'Hari', 'Nale', 'hari@demo.local', d.id, des.id, mgr.id
FROM departments d, designations des, employees mgr
WHERE d.code = 'ENG' AND des.code = 'SE' AND mgr.employee_code = 'EMP-ANKIT';

INSERT INTO users (username, password, email, enabled, employee_id)
SELECT e.id::text,
       '$2a$10$vd0pjEdj7QF9BwnakDcJJue.TUJM6Zk/t3yUqsI05btOQ297PoTCi',
       'hari@demo.local',
       true,
       e.id
FROM employees e
WHERE e.employee_code = 'EMP-HARI';

INSERT INTO users (username, password, email, enabled, employee_id)
SELECT e.id::text,
       '$2a$10$vd0pjEdj7QF9BwnakDcJJue.TUJM6Zk/t3yUqsI05btOQ297PoTCi',
       'ankit@demo.local',
       true,
       e.id
FROM employees e
WHERE e.employee_code = 'EMP-ANKIT';

INSERT INTO users (username, password, email, enabled, employee_id)
VALUES ('soman', '$2a$10$vd0pjEdj7QF9BwnakDcJJue.TUJM6Zk/t3yUqsI05btOQ297PoTCi', 'soman@demo.local', true, NULL);

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.username IN (SELECT id::text FROM employees WHERE employee_code IN ('EMP-HARI', 'EMP-ANKIT'))
  AND r.name IN ('ROLE_USER', 'ROLE_EMPLOYEE');

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.username = 'soman' AND r.name IN ('ROLE_USER', 'ROLE_ADMIN');

INSERT INTO salary_components (code, name, kind, sort_order)
VALUES ('BASIC', 'Basic', 'EARNING', 10),
       ('HRA', 'HRA', 'EARNING', 20),
       ('ALLOWANCE', 'Allowances', 'EARNING', 30),
       ('PF', 'PF', 'DEDUCTION', 100),
       ('TAX', 'Income Tax', 'DEDUCTION', 110),
       ('ADVANCE_RECOVERY', 'Salary advance recovery', 'DEDUCTION', 200),
       ('JOINING_BONUS', 'Joining bonus', 'EARNING', 150),
       ('ANNUAL_BONUS', 'Annual bonus', 'EARNING', 160);

INSERT INTO leave_types (name, code, days_per_year, carry_forward, paid)
VALUES ('Annual Leave', 'ANNUAL', 18, true, true),
       ('Sick Leave', 'SICK', 12, false, true);

INSERT INTO leave_balances (employee_id, leave_type_id, year, allocated_days, used_days, carry_forwarded_days)
SELECT e.id, lt.id, y.yr, lt.days_per_year, 0, 0
FROM employees e
         CROSS JOIN leave_types lt
         CROSS JOIN (VALUES (2025), (2026), (2027)) AS y(yr)
WHERE e.employee_code IN ('EMP-HARI', 'EMP-ANKIT')
  AND lt.active = true;

INSERT INTO holidays (holiday_date, name) VALUES
    ('2025-01-01', 'New Year''s Day'),
    ('2025-01-26', 'Republic Day'),
    ('2025-03-14', 'Holi'),
    ('2025-08-15', 'Independence Day'),
    ('2025-10-02', 'Gandhi Jayanti'),
    ('2025-10-20', 'Dussehra'),
    ('2025-11-01', 'Diwali'),
    ('2025-12-25', 'Christmas'),
    ('2026-01-01', 'New Year''s Day'),
    ('2026-01-26', 'Republic Day'),
    ('2026-03-03', 'Holi'),
    ('2026-08-15', 'Independence Day'),
    ('2026-10-02', 'Gandhi Jayanti'),
    ('2026-10-12', 'Dussehra'),
    ('2026-11-08', 'Diwali'),
    ('2026-12-25', 'Christmas');

INSERT INTO offer_templates (name, body_html, active)
VALUES (
        'Standard offer',
        '<p>Dear {{candidateName}},</p><p>We are pleased to offer you the position of <strong>{{designation}}</strong> in <strong>{{department}}</strong>, reporting to {{managerName}}.</p><p>Proposed start date: {{joinDate}}. Annual CTC: {{currency}} {{annualCtc}}.</p><p>Regards,<br/>HR</p>',
        true
       );
