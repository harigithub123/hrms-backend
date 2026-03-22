package com.hrms.onboarding.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record OnboardingCreateRequest(
        @NotBlank @Size(max = 100) String candidateFirstName,
        @NotBlank @Size(max = 100) String candidateLastName,
        @Size(max = 255) String candidateEmail,
        @NotNull LocalDate joinDate,
        Long departmentId,
        Long designationId,
        Long managerId,
        Long offerId,
        Long assignedHrUserId,
        @Size(max = 2000) String notes
) {}
