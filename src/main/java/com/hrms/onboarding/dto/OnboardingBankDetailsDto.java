package com.hrms.onboarding.dto;

import com.hrms.onboarding.entity.OnboardingBankDetails;

import java.time.Instant;

public record OnboardingBankDetailsDto(
        Long id,
        Long caseId,
        String accountHolderName,
        String bankName,
        String branch,
        String accountNumber,
        String ifscCode,
        String accountType,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {
    public static OnboardingBankDetailsDto from(OnboardingBankDetails b) {
        return new OnboardingBankDetailsDto(
                b.getId(),
                b.getOnboardingCase().getId(),
                b.getAccountHolderName(),
                b.getBankName(),
                b.getBranch(),
                b.getAccountNumber(),
                b.getIfscCode(),
                b.getAccountType().name(),
                b.getNotes(),
                b.getCreatedAt(),
                b.getUpdatedAt()
        );
    }
}
