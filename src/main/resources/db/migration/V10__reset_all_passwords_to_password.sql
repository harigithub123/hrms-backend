-- Earlier migrations used an invalid BCrypt hash (did not match plain text "password").
-- Set password = "password" for every user (Spring BCryptPasswordEncoder, strength 10).
UPDATE users
SET password = '$2a$10$vd0pjEdj7QF9BwnakDcJJue.TUJM6Zk/t3yUqsI05btOQ297PoTCi';
