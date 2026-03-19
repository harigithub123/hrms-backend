package com.hrms.leave.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record LeaveBalanceUpsertRequest(
        @NotNull Long employeeId,
        @NotNull Long leaveTypeId,
        @NotNull Integer year,
        @NotNull BigDecimal allocatedDays
) {}
