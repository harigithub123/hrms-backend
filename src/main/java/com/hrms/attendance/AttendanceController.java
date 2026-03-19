package com.hrms.attendance;

import com.hrms.attendance.dto.AttendanceRecordDto;
import com.hrms.attendance.dto.AttendanceUpsertRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;

    public AttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<AttendanceRecordDto> list(
            @RequestParam Long employeeId,
            @RequestParam LocalDate from,
            @RequestParam LocalDate to
    ) {
        return attendanceService.list(employeeId, from, to);
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public AttendanceRecordDto upsert(@Valid @RequestBody AttendanceUpsertRequest req) {
        return attendanceService.upsert(req);
    }
}
