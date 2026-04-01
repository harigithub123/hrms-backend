package com.hrms.offers.dto;

import com.hrms.offers.OfferStatus;
import com.hrms.offers.entity.JobOffer;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record OfferDto(
        Long id,
        String candidateName,
        String candidateEmail,
        String candidateMobile,
        OfferStatus status,
        String employeeType,
        Long departmentId,
        String departmentName,
        Long designationId,
        String designationName,
        LocalDate joiningDate,
        LocalDate offerReleaseDate,
        Integer probationPeriodMonths,
        BigDecimal joiningBonus,
        BigDecimal yearlyBonus,
        Instant sentAt,
        Instant acceptedAt,
        Instant rejectedAt,
        Instant joinedAt,
        String lastEmailStatus,
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
                o.getProbationPeriodMonths(),
                o.getJoiningBonus(),
                o.getYearlyBonus(),
                o.getSentAt(),
                o.getAcceptedAt(),
                o.getRejectedAt(),
                o.getJoinedAt(),
                o.getLastEmailStatus(),
                o.getEmployee() != null ? o.getEmployee().getId() : null,
                o.getCreatedAt()
        );
    }
}
