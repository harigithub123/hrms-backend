package com.hrms.onboarding.dto;

import com.hrms.onboarding.entity.OnboardingBankDetails;
import com.hrms.payroll.entity.EmployeePayrollBank;

import java.time.Instant;
import java.time.LocalDate;

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
        LocalDate effectiveFrom,
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
                null,
                b.getCreatedAt(),
                b.getUpdatedAt()
        );
    }

    public static OnboardingBankDetailsDto fromEmployeePayrollBank(EmployeePayrollBank b) {
        return new OnboardingBankDetailsDto(
                b.getId(),
                null,
                b.getAccountHolderName(),
                b.getBankName(),
                b.getBranch(),
                b.getAccountNumber(),
                b.getIfscCode(),
                b.getAccountType().name(),
                b.getNotes(),
                b.getEffectiveFrom(),
                b.getCreatedAt(),
                b.getUpdatedAt()
        );
    }
}
