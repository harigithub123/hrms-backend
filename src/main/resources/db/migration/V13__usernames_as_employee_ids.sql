-- Align login usernames with employee id (string) for all employee-linked accounts.
UPDATE users u
SET username = e.id::text
FROM employees e
WHERE u.employee_id = e.id
  AND u.username IS DISTINCT FROM e.id::text;
