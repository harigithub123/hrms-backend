package com.hrms.payroll;

import com.hrms.payroll.dto.PayslipDto;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payroll/payslips")
public class PayslipController {

    private final PayrollService payrollService;

    public PayslipController(PayrollService payrollService) {
        this.payrollService = payrollService;
    }

    @GetMapping("/mine")
    @PreAuthorize("isAuthenticated()")
    public List<PayslipDto> mine(@RequestParam(required = false) Long payRunId) {
        return payrollService.listMyPayslips(payRunId);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public PayslipDto get(@PathVariable Long id) {
        return payrollService.getPayslip(id);
    }

    @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> pdf(@PathVariable Long id) {
        byte[] data = payrollService.payslipPdf(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"payslip-" + id + ".pdf\"")
                .body(data);
    }
}
