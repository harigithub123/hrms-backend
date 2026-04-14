package com.hrms.payroll;

import com.hrms.payroll.dto.SalaryComponentDto;
import com.hrms.payroll.dto.SalaryComponentAdminDto;
import com.hrms.payroll.dto.PayrollFixedComponentUpsertRequest;
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

    @GetMapping("/all-with-fixed")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public List<SalaryComponentAdminDto> listAllWithFixed() {
        return payrollService.listAllComponentsAdminWithFixed();
    }

    @PutMapping("/{componentId}/fixed-monthly-amount")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<Void> setFixedMonthlyAmount(
            @PathVariable Long componentId,
            @Valid @RequestBody PayrollFixedComponentUpsertRequest req
    ) {
        payrollService.setFixedMonthlyAmount(componentId, req);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{componentId}/fixed-monthly-amount")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<Void> clearFixedMonthlyAmount(@PathVariable Long componentId) {
        payrollService.removeFixedComponentAmount(componentId);
        return ResponseEntity.noContent().build();
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
