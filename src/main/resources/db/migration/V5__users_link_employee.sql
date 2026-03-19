-- Optional link between login user and employee (for self-service leave / my payslips)
ALTER TABLE users
    ADD COLUMN employee_id BIGINT REFERENCES employees (id) ON DELETE SET NULL;

CREATE INDEX idx_users_employee_id ON users (employee_id);
