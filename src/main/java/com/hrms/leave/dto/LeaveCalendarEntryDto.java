package com.hrms.leave.dto;

import com.hrms.leave.LeaveRequestStatus;

import java.time.LocalDate;

public record LeaveCalendarEntryDto(
        LocalDate date,
        Long requestId,
        Long employeeId,
        String employeeName,
        Long leaveTypeId,
        String leaveTypeCode,
        String leaveTypeName,
        LeaveRequestStatus status
) {}
