package com.hrms.leave.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record LeaveTypeRequest(
        @NotBlank @Size(max = 150) String name,
        @NotBlank @Size(max = 50) String code,
        @NotNull BigDecimal daysPerYear,
        boolean carryForward,
        boolean paid,
        boolean active
) {}
