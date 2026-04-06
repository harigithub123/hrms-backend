package com.hrms.payroll;

import com.hrms.payroll.dto.EmployeePayrollBankSummaryDto;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * HR-only payroll bank listing. Per-employee updates go through {@code /api/payroll-bank/employees/{id}};
 * duplicate effective dates for the same employee are rejected in {@link EmployeePayrollBankService}.
 */
@RestController
@RequestMapping("/api/payroll-bank/hr")
@PreAuthorize("hasAnyRole('HR','ADMIN')")
public class PayrollBankHrController {

    private final EmployeePayrollBankService employeePayrollBankService;

    public PayrollBankHrController(EmployeePayrollBankService employeePayrollBankService) {
        this.employeePayrollBankService = employeePayrollBankService;
    }

    @GetMapping("/employee-summaries")
    public List<EmployeePayrollBankSummaryDto> listEmployeeSummaries() {
        return employeePayrollBankService.listHrEmployeeSummaries();
    }
}
