package com.hrms.compensation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CompensationCreateRequest(
        @NotNull Long employeeId,
        @NotNull LocalDate effectiveFrom,
        LocalDate effectiveTo,
        BigDecimal annualCtc,
        @Size(max = 2000) String notes,
        @NotEmpty @Valid List<CompensationLineRequest> lines
) {}
