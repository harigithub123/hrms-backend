package com.hrms.payroll.dto;

import com.hrms.payroll.SalaryComponentKind;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SalaryComponentRequest(
        @NotBlank @Size(max = 50) String code,
        @NotBlank @Size(max = 150) String name,
        @NotNull SalaryComponentKind kind,
        int sortOrder,
        boolean active
) {}
