package com.hrms.payroll;

import com.hrms.onboarding.dto.EmployeePayrollBankContextDto;
import com.hrms.onboarding.dto.OnboardingBankDetailsUpsertRequest;
import com.hrms.payroll.dto.PayrollBankAuditDto;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payroll-bank/employees")
@PreAuthorize("hasAnyRole('HR','ADMIN')")
public class PayrollBankController {

    private final EmployeePayrollBankService employeePayrollBankService;

    public PayrollBankController(EmployeePayrollBankService employeePayrollBankService) {
        this.employeePayrollBankService = employeePayrollBankService;
    }

    @GetMapping("/{employeeId}")
    public EmployeePayrollBankContextDto get(@PathVariable Long employeeId) {
        return employeePayrollBankService.getContextForHr(employeeId);
    }

    @PutMapping("/{employeeId}")
    public EmployeePayrollBankContextDto put(
            @PathVariable Long employeeId,
            @Valid @RequestBody OnboardingBankDetailsUpsertRequest req
    ) {
        return employeePayrollBankService.upsertForHr(employeeId, req);
    }

    @GetMapping("/{employeeId}/audits")
    public List<PayrollBankAuditDto> audits(@PathVariable Long employeeId) {
        return employeePayrollBankService.listAuditsForHr(employeeId);
    }
}
