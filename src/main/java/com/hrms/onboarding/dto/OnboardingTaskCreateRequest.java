package com.hrms.onboarding.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OnboardingTaskCreateRequest(
        @NotBlank @Size(max = 300) String name
) {}
