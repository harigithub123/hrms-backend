package com.hrms.onboarding.dto;

/**
 * {@code linked} is true when an onboarding case exists for this employee (informational).
 * Authoritative payroll bank is {@link com.hrms.payroll.entity.EmployeePayrollBank} when present;
 * otherwise bank details may still appear from the onboarding case record.
 */
public record EmployeePayrollBankContextDto(
        boolean linked,
        Long onboardingCaseId,
        OnboardingBankDetailsDto bankDetails
) {
}
