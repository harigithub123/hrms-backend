package com.hrms.leave;

import com.hrms.leave.dto.LeaveBalanceDto;
import com.hrms.leave.dto.LeaveBalanceUpsertRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leave/balances")
public class LeaveBalanceController {

    private final LeaveBalanceService leaveBalanceService;

    public LeaveBalanceController(LeaveBalanceService leaveBalanceService) {
        this.leaveBalanceService = leaveBalanceService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<LeaveBalanceDto> list(
            @RequestParam Long employeeId,
            @RequestParam int year
    ) {
        return leaveBalanceService.list(employeeId, year);
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public LeaveBalanceDto upsert(@Valid @RequestBody LeaveBalanceUpsertRequest req) {
        return leaveBalanceService.upsert(req);
    }
}
