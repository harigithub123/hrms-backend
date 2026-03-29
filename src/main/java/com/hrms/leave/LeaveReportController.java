package com.hrms.leave;

import com.hrms.leave.dto.LeaveLedgerRowDto;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/leave/reports")
public class LeaveReportController {

    private final LeaveReportService leaveReportService;

    public LeaveReportController(LeaveReportService leaveReportService) {
        this.leaveReportService = leaveReportService;
    }

    @GetMapping("/ledger")
    @PreAuthorize("isAuthenticated()")
    public List<LeaveLedgerRowDto> ledger(
            @RequestParam Long employeeId,
            @RequestParam int year
    ) {
        return leaveReportService.ledger(employeeId, year);
    }
}
