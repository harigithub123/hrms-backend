-- Self-service employee role (login accounts created per employee)
INSERT INTO roles (name)
SELECT 'ROLE_EMPLOYEE'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ROLE_EMPLOYEE');

-- Ensure every login already linked to an employee has ROLE_EMPLOYEE
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
CROSS JOIN roles r
WHERE u.employee_id IS NOT NULL
  AND r.name = 'ROLE_EMPLOYEE'
  AND NOT EXISTS (
    SELECT 1 FROM user_roles ur WHERE ur.user_id = u.id AND ur.role_id = r.id
  );

-- At most one user account per employee
CREATE UNIQUE INDEX uq_users_employee_id ON users (employee_id) WHERE employee_id IS NOT NULL;
