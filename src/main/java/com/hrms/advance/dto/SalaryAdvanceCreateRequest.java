package com.hrms.advance.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record SalaryAdvanceCreateRequest(
        Long employeeId,
        @NotNull @Positive BigDecimal amount,
        @Size(max = 2000) String reason,
        @NotNull @Positive int recoveryMonths
) {}
