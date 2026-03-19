-- Leave
CREATE TABLE leave_types (
    id             BIGSERIAL PRIMARY KEY,
    name           VARCHAR(150) NOT NULL,
    code           VARCHAR(50)  NOT NULL UNIQUE,
    days_per_year  NUMERIC(7, 2) NOT NULL DEFAULT 0,
    carry_forward  BOOLEAN      NOT NULL DEFAULT false,
    paid           BOOLEAN      NOT NULL DEFAULT true,
    active         BOOLEAN      NOT NULL DEFAULT true,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE leave_balances (
    id              BIGSERIAL PRIMARY KEY,
    employee_id     BIGINT NOT NULL REFERENCES employees (id) ON DELETE CASCADE,
    leave_type_id   BIGINT NOT NULL REFERENCES leave_types (id) ON DELETE CASCADE,
    year            INT    NOT NULL,
    allocated_days  NUMERIC(7, 2) NOT NULL DEFAULT 0,
    used_days       NUMERIC(7, 2) NOT NULL DEFAULT 0,
    UNIQUE (employee_id, leave_type_id, year)
);

CREATE INDEX idx_leave_balances_employee ON leave_balances (employee_id);

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

-- Attendance
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

-- Payroll: dynamic salary headers (Basic, HRA, Allowances, etc.)
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

-- Seed common salary column headers
INSERT INTO salary_components (code, name, kind, sort_order)
VALUES ('BASIC', 'Basic', 'EARNING', 10),
       ('HRA', 'HRA', 'EARNING', 20),
       ('ALLOWANCE', 'Allowances', 'EARNING', 30),
       ('PF', 'PF', 'DEDUCTION', 100),
       ('TAX', 'Income Tax', 'DEDUCTION', 110);

-- Seed default leave types
INSERT INTO leave_types (name, code, days_per_year, carry_forward, paid)
VALUES ('Annual Leave', 'ANNUAL', 18, true, true),
       ('Sick Leave', 'SICK', 12, false, true);
