-- Professional Tax line item for payslips (amount comes from hrms.payroll.statutory.professional-tax-monthly)
INSERT INTO salary_components (code, name, kind, sort_order)
VALUES ('PT', 'Professional Tax', 'DEDUCTION', 105)
ON CONFLICT (code) DO NOTHING;
