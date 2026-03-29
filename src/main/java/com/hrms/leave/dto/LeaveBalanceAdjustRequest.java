package com.hrms.leave.dto;

import com.hrms.leave.LeaveBalanceAdjustmentKind;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record LeaveBalanceAdjustRequest(
        @NotNull Long employeeId,
        @NotNull Long leaveTypeId,
        @NotNull Integer year,
        @NotNull LeaveBalanceAdjustmentKind kind,
        @NotNull BigDecimal deltaDays,
        @NotBlank @Size(max = 2000) String comment
) {}
