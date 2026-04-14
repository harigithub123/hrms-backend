-- Org-wide fixed monthly amounts applied to every payslip when the component is not already present on the employee compensation.
CREATE TABLE payroll_fixed_component_amounts (
    id                    BIGSERIAL PRIMARY KEY,
    salary_component_id   BIGINT         NOT NULL UNIQUE REFERENCES salary_components (id) ON DELETE CASCADE,
    monthly_amount        NUMERIC(14, 2) NOT NULL,
    updated_at            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Defaults aligned with previous application.yml (hrms.payroll.statutory.*)
INSERT INTO payroll_fixed_component_amounts (salary_component_id, monthly_amount)
SELECT id, 1800 FROM salary_components WHERE UPPER(code) = 'PF'
ON CONFLICT (salary_component_id) DO NOTHING;

INSERT INTO payroll_fixed_component_amounts (salary_component_id, monthly_amount)
SELECT id, 200 FROM salary_components WHERE UPPER(code) = 'PT'
ON CONFLICT (salary_component_id) DO NOTHING;
