package com.hrms.payroll;

import com.hrms.payroll.dto.SalaryComponentDto;
import com.hrms.payroll.dto.SalaryComponentRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payroll/components")
public class SalaryComponentController {

    private final PayrollService payrollService;

    public SalaryComponentController(PayrollService payrollService) {
        this.payrollService = payrollService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<SalaryComponentDto> listActive() {
        return payrollService.listComponents();
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public List<SalaryComponentDto> listAll() {
        return payrollService.listAllComponentsAdmin();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<SalaryComponentDto> create(@Valid @RequestBody SalaryComponentRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(payrollService.createComponent(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public SalaryComponentDto update(@PathVariable Long id, @Valid @RequestBody SalaryComponentRequest req) {
        return payrollService.updateComponent(id, req);
    }
}
