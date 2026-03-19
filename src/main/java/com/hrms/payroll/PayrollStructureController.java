package com.hrms.payroll;

import com.hrms.payroll.dto.SalaryStructureDto;
import com.hrms.payroll.dto.SalaryStructureRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/payroll/structures")
public class PayrollStructureController {

    private final PayrollService payrollService;

    public PayrollStructureController(PayrollService payrollService) {
        this.payrollService = payrollService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<SalaryStructureDto> save(@Valid @RequestBody SalaryStructureRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(payrollService.saveStructure(req));
    }

    @GetMapping("/employee/{employeeId}")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public SalaryStructureDto latest(
            @PathVariable Long employeeId,
            @RequestParam(required = false) LocalDate asOf
    ) {
        return payrollService.getLatestStructureForEmployee(employeeId, asOf);
    }
}
