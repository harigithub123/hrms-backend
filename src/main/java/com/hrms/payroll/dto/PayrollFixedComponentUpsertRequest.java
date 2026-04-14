package com.hrms.payroll.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PayrollFixedComponentUpsertRequest(
        @NotNull BigDecimal monthlyAmount
) {}
