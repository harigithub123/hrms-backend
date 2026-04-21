-- Store payroll period as calendar year + month instead of date range.

ALTER TABLE pay_runs ADD COLUMN pay_year INTEGER;
ALTER TABLE pay_runs ADD COLUMN pay_month INTEGER;

UPDATE pay_runs SET
    pay_year = EXTRACT(YEAR FROM period_start)::int,
    pay_month = EXTRACT(MONTH FROM period_start)::int;

ALTER TABLE pay_runs ALTER COLUMN pay_year SET NOT NULL;
ALTER TABLE pay_runs ALTER COLUMN pay_month SET NOT NULL;

ALTER TABLE pay_runs DROP COLUMN period_start;
ALTER TABLE pay_runs DROP COLUMN period_end;

ALTER TABLE pay_runs ADD CONSTRAINT chk_pay_runs_pay_month CHECK (pay_month >= 1 AND pay_month <= 12);

CREATE UNIQUE INDEX uq_pay_runs_pay_year_month ON pay_runs (pay_year, pay_month);
