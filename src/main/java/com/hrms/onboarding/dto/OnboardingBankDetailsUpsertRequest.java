package com.hrms.onboarding.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OnboardingBankDetailsUpsertRequest(
        @NotBlank @Size(max = 200) String accountHolderName,
        @NotBlank @Size(max = 200) String bankName,
        @Size(max = 200) String branch,
        @NotBlank @Size(max = 80) String accountNumber,
        @NotBlank @Size(max = 20) String ifscCode,
        @NotBlank @Size(max = 20) String accountType,
        @Size(max = 500) String notes
) {}
