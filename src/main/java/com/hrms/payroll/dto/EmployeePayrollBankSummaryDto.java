package com.hrms.payroll.dto;

import com.hrms.onboarding.dto.OnboardingBankDetailsDto;
import com.hrms.org.entity.Employee;

public record EmployeePayrollBankSummaryDto(
        long employeeId,
        String firstName,
        String lastName,
        String employeeCode,
        String departmentName,
        OnboardingBankDetailsDto bankDetails
) {
    public static EmployeePayrollBankSummaryDto of(Employee e, OnboardingBankDetailsDto bankDetails) {
        String dept = e.getDepartment() != null ? e.getDepartment().getName() : null;
        return new EmployeePayrollBankSummaryDto(
                e.getId(),
                e.getFirstName(),
                e.getLastName(),
                e.getEmployeeCode(),
                dept,
                bankDetails
        );
    }
}
