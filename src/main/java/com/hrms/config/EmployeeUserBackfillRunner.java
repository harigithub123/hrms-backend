package com.hrms.config;

import com.hrms.auth.EmployeeAccountService;
import com.hrms.org.entity.Employee;
import com.hrms.org.repository.EmployeeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates login accounts for employees that existed before automatic provisioning.
 */
@Component
@Order(100)
public class EmployeeUserBackfillRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(EmployeeUserBackfillRunner.class);

    private final EmployeeRepository employeeRepository;
    private final EmployeeAccountService employeeAccountService;

    public EmployeeUserBackfillRunner(
            EmployeeRepository employeeRepository,
            EmployeeAccountService employeeAccountService
    ) {
        this.employeeRepository = employeeRepository;
        this.employeeAccountService = employeeAccountService;
    }

    @Override
    @Transactional
    public void run(String... args) {
        for (Employee e : employeeRepository.findAll()) {
            try {
                employeeAccountService.ensureUserExistsForEmployee(e.getId());
            } catch (Exception ex) {
                log.warn("Could not ensure user for employee {}: {}", e.getId(), ex.getMessage());
            }
        }
    }
}
