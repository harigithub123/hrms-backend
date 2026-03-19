package com.hrms;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BcryptHashVerifyTest {

    /** Must match Flyway migrations (password = "password"). */
    static final String PASSWORD_BCRYPT = "$2a$10$vd0pjEdj7QF9BwnakDcJJue.TUJM6Zk/t3yUqsI05btOQ297PoTCi";

    @Test
    void migrationHashMatchesPassword() {
        var enc = new BCryptPasswordEncoder();
        assertTrue(enc.matches("password", PASSWORD_BCRYPT), "Migration BCrypt hash must verify for password=password");
    }
}
