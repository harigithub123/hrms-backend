package com.hrms.org;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record EmployeeRequest(
        @Size(max = 50) String employeeCode,
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @Size(max = 255) String email,
        @Size(max = 30) String mobileNumber,
        Long departmentId,
        Long designationId,
        Long managerId,
        LocalDate joinedAt
) {}
