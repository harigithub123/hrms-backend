package com.hrms.onboarding.dto;

import com.hrms.onboarding.OnboardingStatus;
import com.hrms.onboarding.entity.OnboardingCase;

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
        Long managerId,
        String managerName,
        Long employeeId,
        Long offerId,
        Long assignedHrUserId,
        String notes,
        Instant createdAt,
        List<OnboardingTaskDto> tasks
) {
    public static OnboardingCaseDto from(OnboardingCase c, List<OnboardingTaskDto> tasks) {
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
                c.getManager() != null ? c.getManager().getId() : null,
                c.getManager() != null ? (c.getManager().getFirstName() + " " + c.getManager().getLastName()).trim() : null,
                c.getEmployee() != null ? c.getEmployee().getId() : null,
                c.getOffer() != null ? c.getOffer().getId() : null,
                c.getAssignedHr() != null ? c.getAssignedHr().getId() : null,
                c.getNotes(),
                c.getCreatedAt(),
                tasks
        );
    }
}
