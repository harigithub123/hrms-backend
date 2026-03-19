package com.hrms.leave;

import com.hrms.leave.dto.LeaveCalendarEntryDto;
import com.hrms.leave.dto.LeaveCalendarRangeDto;
import com.hrms.leave.dto.LeaveDecisionRequest;
import com.hrms.leave.dto.LeaveRequestCreateRequest;
import com.hrms.leave.dto.LeaveRequestDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/leave")
public class LeaveRequestController {

    private final LeaveRequestService leaveRequestService;

    public LeaveRequestController(LeaveRequestService leaveRequestService) {
        this.leaveRequestService = leaveRequestService;
    }

    @GetMapping("/requests")
    @PreAuthorize("isAuthenticated()")
    public List<LeaveRequestDto> listRequests(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) LeaveRequestStatus status
    ) {
        return leaveRequestService.list(employeeId, status);
    }

    @GetMapping("/requests/pending")
    @PreAuthorize("isAuthenticated()")
    public List<LeaveRequestDto> listPendingForApprover() {
        return leaveRequestService.listPendingForApprover();
    }

    @PostMapping("/requests")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LeaveRequestDto> create(@Valid @RequestBody LeaveRequestCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(leaveRequestService.create(req));
    }

    @PutMapping("/requests/{id}/decision")
    @PreAuthorize("isAuthenticated()")
    public LeaveRequestDto decide(@PathVariable Long id, @Valid @RequestBody LeaveDecisionRequest req) {
        return leaveRequestService.decide(id, req);
    }

    /**
     * Default calendar: one row per leave request (date range). Use for month / list views.
     */
    @GetMapping("/calendar")
    @PreAuthorize("isAuthenticated()")
    public List<LeaveCalendarRangeDto> calendarRanges(
            @RequestParam LocalDate from,
            @RequestParam LocalDate to,
            @RequestParam(required = false) Long employeeId
    ) {
        return leaveRequestService.calendarRanges(from, to, employeeId);
    }

    /**
     * Optional: one row per day (expanded), e.g. for day-by-day reporting.
     */
    @GetMapping("/calendar/days")
    @PreAuthorize("isAuthenticated()")
    public List<LeaveCalendarEntryDto> calendarDays(
            @RequestParam LocalDate from,
            @RequestParam LocalDate to,
            @RequestParam(required = false) Long employeeId
    ) {
        return leaveRequestService.calendarDays(from, to, employeeId);
    }
}
