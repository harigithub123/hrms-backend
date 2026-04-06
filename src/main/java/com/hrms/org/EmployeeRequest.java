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
        LocalDate joinedAt,
        /** When set, updates status; when null, existing value is unchanged on update. */
        EmploymentStatus employmentStatus,
        /** When set (including on first save), updates last working day; omit to leave unchanged. */
        LocalDate lastWorkingDate,
        @Size(max = 1000) String exitReason
) {}
