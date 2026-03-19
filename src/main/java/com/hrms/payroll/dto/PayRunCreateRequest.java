package com.hrms.payroll.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record PayRunCreateRequest(
        @NotNull LocalDate periodStart,
        @NotNull LocalDate periodEnd
) {}
