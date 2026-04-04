package com.hrms.onboarding.dto;

/**
 * Payroll bank details are stored on the onboarding case linked to an employee (after hire).
 * Employees created only via the directory may have no linked case ({@code linked} false).
 */
public record EmployeePayrollBankContextDto(
        boolean linked,
        Long onboardingCaseId,
        OnboardingBankDetailsDto bankDetails
) {
}
