package com.hrms.payroll.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record SalaryStructureRequest(
        @NotNull Long employeeId,
        @NotNull LocalDate effectiveFrom,
        LocalDate effectiveTo,
        boolean isActive,
        @Size(max = 500) String note,
        @NotEmpty @Valid List<SalaryStructureLineRequest> lines
) {}
