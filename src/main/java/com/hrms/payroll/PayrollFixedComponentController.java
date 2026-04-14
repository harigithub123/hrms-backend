package com.hrms.payroll;

import com.hrms.payroll.dto.PayrollFixedComponentDto;
import com.hrms.payroll.dto.PayrollFixedComponentUpsertRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payroll/fixed-components")
public class PayrollFixedComponentController {

    private final PayrollService payrollService;

    public PayrollFixedComponentController(PayrollService payrollService) {
        this.payrollService = payrollService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public List<PayrollFixedComponentDto> list() {
        return payrollService.listFixedComponentAmounts();
    }

    @PutMapping("/{componentId}")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public PayrollFixedComponentDto upsert(
            @PathVariable Long componentId,
            @Valid @RequestBody PayrollFixedComponentUpsertRequest req
    ) {
        return payrollService.upsertFixedComponentAmount(componentId, req);
    }

    @DeleteMapping("/{componentId}")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long componentId) {
        payrollService.removeFixedComponentAmount(componentId);
        return ResponseEntity.noContent().build();
    }
}
