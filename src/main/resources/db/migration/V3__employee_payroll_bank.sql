-- Authoritative payroll bank per employee, audit trail, and employee-initiated change requests.

CREATE TABLE IF NOT EXISTS employee_payroll_bank (
    id                   BIGSERIAL PRIMARY KEY,
    employee_id          BIGINT NOT NULL UNIQUE REFERENCES employees (id) ON DELETE CASCADE,
    account_holder_name  VARCHAR(200) NOT NULL,
    bank_name            VARCHAR(200) NOT NULL,
    branch               VARCHAR(200),
    account_number       VARCHAR(80) NOT NULL,
    ifsc_code            VARCHAR(20) NOT NULL,
    account_type         VARCHAR(20) NOT NULL DEFAULT 'SAVINGS',
    notes                VARCHAR(500),
    effective_from       DATE NOT NULL DEFAULT CURRENT_DATE,
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_employee_payroll_bank_effective ON employee_payroll_bank (employee_id, effective_from DESC);

CREATE TABLE IF NOT EXISTS employee_payroll_bank_audit (
    id                    BIGSERIAL PRIMARY KEY,
    employee_id           BIGINT NOT NULL REFERENCES employees (id) ON DELETE CASCADE,
    action                VARCHAR(50) NOT NULL,
    detail                VARCHAR(2000),
    created_by_user_id    BIGINT REFERENCES users (id) ON DELETE SET NULL,
    created_by_username   VARCHAR(100),
    created_at            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_epb_audit_employee ON employee_payroll_bank_audit (employee_id, created_at DESC);

CREATE TABLE IF NOT EXISTS bank_change_requests (
    id                      BIGSERIAL PRIMARY KEY,
    employee_id             BIGINT NOT NULL REFERENCES employees (id) ON DELETE CASCADE,
    status                  VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    account_holder_name     VARCHAR(200) NOT NULL,
    bank_name               VARCHAR(200) NOT NULL,
    branch                  VARCHAR(200),
    account_number          VARCHAR(80) NOT NULL,
    ifsc_code               VARCHAR(20) NOT NULL,
    account_type            VARCHAR(20) NOT NULL,
    notes                   VARCHAR(500),
    requested_effective_from DATE NOT NULL,
    employee_comment        VARCHAR(1000),
    hr_comment              VARCHAR(1000),
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at             TIMESTAMP WITH TIME ZONE,
    reviewed_by_user_id     BIGINT REFERENCES users (id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_bank_change_requests_status ON bank_change_requests (status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_bank_change_requests_employee ON bank_change_requests (employee_id, status);

-- Seed employee_payroll_bank from existing onboarding-linked hires (latest case per employee).
INSERT INTO employee_payroll_bank (
    employee_id,
    account_holder_name,
    bank_name,
    branch,
    account_number,
    ifsc_code,
    account_type,
    notes,
    effective_from,
    created_at,
    updated_at
)
SELECT DISTINCT ON (e.id)
    e.id,
    obd.account_holder_name,
    obd.bank_name,
    obd.branch,
    obd.account_number,
    obd.ifsc_code,
    obd.account_type,
    obd.notes,
    COALESCE(e.joined_at, obd.created_at::date),
    obd.created_at,
    obd.updated_at
FROM onboarding_bank_details obd
         INNER JOIN onboarding_cases oc ON oc.id = obd.case_id
         INNER JOIN employees e ON e.id = oc.employee_id
WHERE oc.employee_id IS NOT NULL
ORDER BY e.id, oc.id DESC;
