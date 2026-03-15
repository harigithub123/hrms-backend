-- Ensure default admin user has ROLE_HR so they can access Departments, Designations, Employees (role_id 2 = ROLE_HR)
INSERT INTO user_roles (user_id, role_id)
SELECT 1, 2 WHERE NOT EXISTS (SELECT 1 FROM user_roles WHERE user_id = 1 AND role_id = 2);
