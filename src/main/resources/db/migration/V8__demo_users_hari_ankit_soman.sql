-- Demo accounts (password for all: password)
-- Hari = employee (reports to Ankit), Ankit = manager, Soman = admin
-- BCrypt hash below matches V2 default admin password

INSERT INTO roles (name)
SELECT 'ROLE_MANAGER'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ROLE_MANAGER');

INSERT INTO departments (name, code)
SELECT 'Engineering', 'ENG'
WHERE NOT EXISTS (SELECT 1 FROM departments WHERE code = 'ENG');

INSERT INTO designations (name, code)
SELECT 'Engineering Manager', 'MGR'
WHERE NOT EXISTS (SELECT 1 FROM designations WHERE code = 'MGR');

INSERT INTO designations (name, code)
SELECT 'Software Engineer', 'SE'
WHERE NOT EXISTS (SELECT 1 FROM designations WHERE code = 'SE');

INSERT INTO employees (employee_code, first_name, last_name, email, department_id, designation_id, manager_id)
SELECT 'EMP-ANKIT', 'Ankit', 'Kumar', 'ankit@demo.local', d.id, des.id, NULL
FROM departments d, designations des
WHERE d.code = 'ENG' AND des.code = 'MGR'
  AND NOT EXISTS (SELECT 1 FROM employees WHERE employee_code = 'EMP-ANKIT');

INSERT INTO employees (employee_code, first_name, last_name, email, department_id, designation_id, manager_id)
SELECT 'EMP-HARI', 'Hari', 'Nale', 'hari@demo.local', d.id, des.id, mgr.id
FROM departments d, designations des, employees mgr
WHERE d.code = 'ENG' AND des.code = 'SE' AND mgr.employee_code = 'EMP-ANKIT'
  AND NOT EXISTS (SELECT 1 FROM employees WHERE employee_code = 'EMP-HARI');

INSERT INTO users (username, password, email, enabled, employee_id)
SELECT 'hari',
       '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqYQXkHOQj.5/zYVVBvWZ.lbQK.By',
       'hari@demo.local',
       true,
       e.id
FROM employees e
WHERE e.employee_code = 'EMP-HARI'
  AND NOT EXISTS (SELECT 1 FROM users WHERE username = 'hari');

INSERT INTO users (username, password, email, enabled, employee_id)
SELECT 'ankit',
       '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqYQXkHOQj.5/zYVVBvWZ.lbQK.By',
       'ankit@demo.local',
       true,
       e.id
FROM employees e
WHERE e.employee_code = 'EMP-ANKIT'
  AND NOT EXISTS (SELECT 1 FROM users WHERE username = 'ankit');

INSERT INTO users (username, password, email, enabled, employee_id)
SELECT 'soman',
       '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqYQXkHOQj.5/zYVVBvWZ.lbQK.By',
       'soman@demo.local',
       true,
       NULL
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'soman');

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.username = 'hari' AND r.name = 'ROLE_USER'
  AND NOT EXISTS (SELECT 1 FROM user_roles ur WHERE ur.user_id = u.id AND ur.role_id = r.id);

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.username = 'ankit' AND r.name IN ('ROLE_USER', 'ROLE_MANAGER')
  AND NOT EXISTS (SELECT 1 FROM user_roles ur WHERE ur.user_id = u.id AND ur.role_id = r.id);

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.username = 'soman' AND r.name IN ('ROLE_USER', 'ROLE_ADMIN')
  AND NOT EXISTS (SELECT 1 FROM user_roles ur WHERE ur.user_id = u.id AND ur.role_id = r.id);

-- Leave balances for Hari & Ankit (years aligned with app / holidays)
INSERT INTO leave_balances (employee_id, leave_type_id, year, allocated_days, used_days)
SELECT e.id, lt.id, y.yr, lt.days_per_year, 0
FROM employees e
         CROSS JOIN leave_types lt
         CROSS JOIN (VALUES (2025), (2026), (2027)) AS y(yr)
WHERE e.employee_code IN ('EMP-HARI', 'EMP-ANKIT')
  AND lt.active = true
  AND NOT EXISTS (SELECT 1
                  FROM leave_balances lb
                  WHERE lb.employee_id = e.id
                    AND lb.leave_type_id = lt.id
                    AND lb.year = y.yr);
