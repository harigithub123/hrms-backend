package com.hrms.payroll;

import com.hrms.payroll.dto.PayRunCreateRequest;
import com.hrms.payroll.dto.PayRunDto;
import com.hrms.payroll.dto.PayslipDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payroll/runs")
public class PayRunController {

    private final PayrollService payrollService;

    public PayRunController(PayrollService payrollService) {
        this.payrollService = payrollService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public List<PayRunDto> list() {
        return payrollService.listPayRuns();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<PayRunDto> create(@Valid @RequestBody PayRunCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(payrollService.createPayRun(req));
    }

    @GetMapping("/{id}/payslips")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public List<PayslipDto> payslips(@PathVariable Long id) {
        return payrollService.listPayslipsForRun(id);
    }
}
