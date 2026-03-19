-- Team features use employee hierarchy (manager_id), not ROLE_MANAGER
DELETE FROM user_roles
WHERE role_id = (SELECT id FROM roles WHERE name = 'ROLE_MANAGER');

DELETE FROM roles WHERE name = 'ROLE_MANAGER';
