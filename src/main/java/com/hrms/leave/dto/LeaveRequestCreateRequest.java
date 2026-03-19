package com.hrms.leave.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record LeaveRequestCreateRequest(
        /** When set, HR/ADMIN may create on behalf of another employee */
        Long employeeId,
        @NotNull Long leaveTypeId,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        String reason
) {}
