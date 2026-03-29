package com.hrms.config;

import com.hrms.auth.entity.Role;
import com.hrms.auth.entity.User;
import com.hrms.auth.repository.RoleRepository;
import com.hrms.auth.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Ensures the system administrator exists and can sign in with {@code admin} / {@code password}.
 * Repairs username/password if migrations or data changes altered the admin row.
 */
@Component
@Order(5)
public class AdminUserSeeder implements CommandLineRunner {

    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "password";
    private static final String ADMIN_EMAIL = "admin@hrms.local";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminUserSeeder(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        userRepository.findByUsername(ADMIN_USERNAME).ifPresentOrElse(
                this::repairAdminUser,
                this::ensureAdminUserExists
        );
    }

    private void repairAdminUser(User user) {
        boolean dirty = false;
        if (user.getEmployee() != null) {
            user.setEmployee(null);
            dirty = true;
        }
        if (!user.isEnabled()) {
            user.setEnabled(true);
            dirty = true;
        }
        if (!passwordEncoder.matches(ADMIN_PASSWORD, user.getPassword())) {
            user.setPassword(passwordEncoder.encode(ADMIN_PASSWORD));
            dirty = true;
        }
        if (dirty) {
            userRepository.save(user);
        }
    }

    private void ensureAdminUserExists() {
        List<User> byEmail = userRepository.findByEmailIgnoreCase(ADMIN_EMAIL);
        if (byEmail.size() == 1) {
            User u = byEmail.get(0);
            u.setUsername(ADMIN_USERNAME);
            u.setEmployee(null);
            u.setEnabled(true);
            u.setPassword(passwordEncoder.encode(ADMIN_PASSWORD));
            userRepository.save(u);
            return;
        }
        if (!byEmail.isEmpty()) {
            return;
        }
        User u = new User();
        u.setUsername(ADMIN_USERNAME);
        u.setPassword(passwordEncoder.encode(ADMIN_PASSWORD));
        u.setEmail(ADMIN_EMAIL);
        u.setEnabled(true);
        Role admin = roleRepository.findByName("ROLE_ADMIN").orElseThrow();
        Role user = roleRepository.findByName("ROLE_USER").orElseThrow();
        u.getRoles().add(admin);
        u.getRoles().add(user);
        userRepository.save(u);
    }
}
