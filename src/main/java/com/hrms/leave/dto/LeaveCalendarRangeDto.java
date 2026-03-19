package com.hrms.leave.dto;

import com.hrms.leave.LeaveRequestStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One row per leave request overlapping the calendar window (no per-day expansion).
 */
public record LeaveCalendarRangeDto(
        Long requestId,
        Long employeeId,
        String employeeName,
        Long leaveTypeId,
        String leaveTypeCode,
        String leaveTypeName,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal totalDays,
        LeaveRequestStatus status
) {}
