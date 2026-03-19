package com.hrms.payroll.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record SalaryStructureLineRequest(
        @NotNull Long componentId,
        @NotNull BigDecimal amount
) {}
