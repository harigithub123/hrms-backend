package com.hrms.offers.dto;

import com.hrms.offers.EmployeeType;
import com.hrms.offers.OfferStatus;
import com.hrms.offers.entity.JobOffer;

import java.time.LocalDate;
import java.time.Instant;

public record OfferDto(
        Long id,
        String candidateName,
        String candidateEmail,
        String candidateMobile,
        OfferStatus status,
        EmployeeType employeeType,
        Long departmentId,
        String departmentName,
        Long designationId,
        String designationName,
        LocalDate joiningDate,
        LocalDate offerReleaseDate,
        LocalDate actualJoiningDate,
        Integer probationPeriodMonths,
        Long employeeId,
        Instant createdAt
) {
    public static OfferDto from(JobOffer o) {
        return new OfferDto(
                o.getId(),
                o.getCandidateName(),
                o.getCandidateEmail(),
                o.getCandidateMobile(),
                o.getStatus(),
                o.getEmployeeType(),
                o.getDepartment() != null ? o.getDepartment().getId() : null,
                o.getDepartment() != null ? o.getDepartment().getName() : null,
                o.getDesignation() != null ? o.getDesignation().getId() : null,
                o.getDesignation() != null ? o.getDesignation().getName() : null,
                o.getJoiningDate(),
                o.getOfferReleaseDate(),
                o.getActualJoiningDate(),
                o.getProbationPeriodMonths(),
                o.getEmployee() != null ? o.getEmployee().getId() : null,
                o.getCreatedAt()
        );
    }
}
