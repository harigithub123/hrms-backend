-- Ensure the built-in administrator can sign in as username "admin" / password from V10.
-- V13 may have renamed usernames to employee ids for any row with employee_id; the admin account
-- must never use employee id as username (login is always "admin" for this row).
UPDATE users
SET username = 'admin',
    password = '$2a$10$vd0pjEdj7QF9BwnakDcJJue.TUJM6Zk/t3yUqsI05btOQ297PoTCi',
    enabled    = true,
    employee_id = NULL
WHERE email = 'admin@hrms.local';

-- If email was changed but username is still wrong, fix row that was originally admin (id = 1 from V2)
UPDATE users
SET username = 'admin',
    password = '$2a$10$vd0pjEdj7QF9BwnakDcJJue.TUJM6Zk/t3yUqsI05btOQ297PoTCi',
    enabled    = true,
    employee_id = NULL
WHERE id = 1
  AND (username <> 'admin' OR employee_id IS NOT NULL);
