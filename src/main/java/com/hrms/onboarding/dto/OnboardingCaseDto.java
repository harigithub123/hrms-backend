package com.hrms.onboarding.dto;

import com.hrms.onboarding.OnboardingStatus;
import com.hrms.onboarding.entity.OnboardingCase;
import com.hrms.org.EmploymentStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record OnboardingCaseDto(
        Long id,
        OnboardingStatus status,
        String candidateFirstName,
        String candidateLastName,
        String candidateEmail,
        LocalDate joinDate,
        Long departmentId,
        String departmentName,
        Long designationId,
        String designationName,
        Long employeeId,
        EmploymentStatus employeeEmploymentStatus,
        /** Last working date from linked employee; null if no employee. */
        LocalDate employeeLastWorkingDate,
        /**
         * When linked employee exists: true if today is on or after last working date (exit documents may be completed).
         * false if LWD missing or still in the future; null if no employee on case.
         */
        Boolean exitDocumentTasksEligible,
        Long offerId,
        Long assignedHrUserId,
        String notes,
        Instant createdAt,
        List<OnboardingTaskDto> tasks,
        OnboardingBankDetailsDto bankDetails
) {
    public static OnboardingCaseDto from(
            OnboardingCase c,
            List<OnboardingTaskDto> tasks,
            OnboardingBankDetailsDto bankDetails
    ) {
        return new OnboardingCaseDto(
                c.getId(),
                c.getStatus(),
                c.getCandidateFirstName(),
                c.getCandidateLastName(),
                c.getCandidateEmail(),
                c.getJoinDate(),
                c.getDepartment() != null ? c.getDepartment().getId() : null,
                c.getDepartment() != null ? c.getDepartment().getName() : null,
                c.getDesignation() != null ? c.getDesignation().getId() : null,
                c.getDesignation() != null ? c.getDesignation().getName() : null,
                c.getEmployee() != null ? c.getEmployee().getId() : null,
                c.getEmployee() != null ? c.getEmployee().getEmploymentStatus() : null,
                c.getEmployee() != null ? c.getEmployee().getLastWorkingDate() : null,
                exitDocumentEligibility(c),
                c.getOffer() != null ? c.getOffer().getId() : null,
                c.getAssignedHr() != null ? c.getAssignedHr().getId() : null,
                c.getNotes(),
                c.getCreatedAt(),
                tasks,
                bankDetails
        );
    }

    private static Boolean exitDocumentEligibility(OnboardingCase c) {
        if (c.getEmployee() == null) {
            return null;
        }
        LocalDate lwd = c.getEmployee().getLastWorkingDate();
        if (lwd == null) {
            return false;
        }
        return !LocalDate.now().isBefore(lwd);
    }
}
