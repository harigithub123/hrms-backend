ALTER TABLE employees
    ADD COLUMN employment_status VARCHAR(40) NOT NULL DEFAULT 'JOINED';

UPDATE employees
SET employment_status = 'RESIGNED'
WHERE existed_firm = TRUE;

ALTER TABLE employees
    DROP COLUMN existed_firm;
