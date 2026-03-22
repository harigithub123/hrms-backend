package com.hrms.advance;

import com.hrms.advance.dto.*;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/advances")
public class SalaryAdvanceController {

    private final SalaryAdvanceService salaryAdvanceService;

    public SalaryAdvanceController(SalaryAdvanceService salaryAdvanceService) {
        this.salaryAdvanceService = salaryAdvanceService;
    }

    @GetMapping("/mine")
    @PreAuthorize("isAuthenticated()")
    public List<SalaryAdvanceDto> mine() {
        return salaryAdvanceService.listMine();
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    public List<SalaryAdvanceDto> listAll() {
        return salaryAdvanceService.listAll();
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public SalaryAdvanceDto create(@Valid @RequestBody SalaryAdvanceCreateRequest req) {
        return salaryAdvanceService.create(req);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("isAuthenticated()")
    public SalaryAdvanceDto approve(
            @PathVariable Long id,
            @RequestBody(required = false) SalaryAdvanceApproveRequest req
    ) {
        SalaryAdvanceApproveRequest body = req != null ? req : new SalaryAdvanceApproveRequest(0, null);
        return salaryAdvanceService.approve(id, body);
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("isAuthenticated()")
    public SalaryAdvanceDto reject(
            @PathVariable Long id,
            @RequestBody(required = false) SalaryAdvanceRejectRequest req
    ) {
        return salaryAdvanceService.reject(id, req != null ? req : new SalaryAdvanceRejectRequest(null));
    }

    @PostMapping("/{id}/mark-paid")
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    public SalaryAdvanceDto markPaid(@PathVariable Long id, @RequestParam(required = false) LocalDate payoutDate) {
        return salaryAdvanceService.markPaid(id, payoutDate);
    }
}
