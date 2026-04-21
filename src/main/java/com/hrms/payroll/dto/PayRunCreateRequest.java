package com.hrms.payroll.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PayRunCreateRequest(
        @NotNull @Min(1970) @Max(2200) Integer year,
        @NotNull @Min(1) @Max(12) Integer month
) {}
