package com.hrms.auth;

import com.hrms.auth.entity.Role;
import com.hrms.auth.entity.User;
import com.hrms.auth.repository.RoleRepository;
import com.hrms.auth.repository.UserRepository;
import com.hrms.org.entity.Employee;
import com.hrms.org.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmployeeAccountService {

    public static final String ROLE_EMPLOYEE = "ROLE_EMPLOYEE";

    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final String defaultEmployeePassword;

    public EmployeeAccountService(
            UserRepository userRepository,
            EmployeeRepository employeeRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            @Value("${hrms.employee.default-password:password}") String defaultEmployeePassword
    ) {
        this.userRepository = userRepository;
        this.employeeRepository = employeeRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.defaultEmployeePassword = defaultEmployeePassword;
    }

    /**
     * Login username is always the employee's id as a string (e.g. {@code "42"}). No other scheme.
     */
    @Transactional
    public User provisionUserForNewEmployee(Employee employee) {
        if (employee.getId() == null) {
            throw new IllegalStateException("Employee must be persisted before creating a user account");
        }
        Role employeeRole = roleRepository.findByName(ROLE_EMPLOYEE)
                .orElseThrow(() -> new IllegalStateException("Missing role: " + ROLE_EMPLOYEE));
        User user = new User();
        user.setUsername(String.valueOf(employee.getId()));
        user.setPassword(passwordEncoder.encode(defaultEmployeePassword));
        user.setEmail(employee.getEmail());
        user.setEmployee(employee);
        user.getRoles().add(employeeRole);
        return userRepository.save(user);
    }

    /** Keeps {@link User#getEmail()} in sync when the employee record's email changes (sign-in by email uses this). */
    @Transactional
    public void syncEmailFromEmployee(Employee employee) {
        userRepository.findByEmployeeId(employee.getId()).ifPresent(u -> {
            u.setEmail(employee.getEmail());
            userRepository.save(u);
        });
    }

    /**
     * Startup backfill: create login for employees that still have no user (e.g. created before auto-provisioning).
     */
    @Transactional
    public void ensureUserExistsForEmployee(Long employeeId) {
        if (userRepository.findByEmployeeId(employeeId).isPresent()) {
            return;
        }
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + employeeId));
        provisionUserForNewEmployee(employee);
    }
}
