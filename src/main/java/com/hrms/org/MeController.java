package com.hrms.org;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/me")
@PreAuthorize("isAuthenticated()")
public class MeController {

    private final EmployeeService employeeService;

    public MeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    /**
     * Employees who report to the logged-in user (same as {@code manager_id} = current user's employee id).
     */
    @GetMapping("/direct-reports")
    public List<EmployeeDto> directReports() {
        return employeeService.findDirectReportsForCurrentUser();
    }
}
