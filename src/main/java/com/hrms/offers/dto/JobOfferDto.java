package com.hrms.offers.dto;

import com.hrms.offers.OfferStatus;
import com.hrms.offers.entity.JobOffer;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record JobOfferDto(
        Long id,
        Long templateId,
        String candidateName,
        String candidateEmail,
        String candidateMobile,
        OfferStatus status,
        String employeeType,
        Long departmentId,
        String departmentName,
        Long designationId,
        String designationName,
        Long managerId,
        String managerName,
        LocalDate joinDate,
        LocalDate offerReleaseDate,
        Integer probationPeriodMonths,
        BigDecimal joiningBonus,
        BigDecimal yearlyBonus,
        BigDecimal annualCtc,
        String currency,
        String bodyHtml,
        Instant pdfGeneratedAt,
        Instant sentAt,
        Instant acceptedAt,
        Instant rejectedAt,
        Instant joinedAt,
        String lastEmailStatus,
        Long employeeId,
        Instant createdAt
) {
    public static JobOfferDto from(JobOffer o) {
        return new JobOfferDto(
                o.getId(),
                o.getTemplate() != null ? o.getTemplate().getId() : null,
                o.getCandidateName(),
                o.getCandidateEmail(),
                o.getCandidateMobile(),
                o.getStatus(),
                o.getEmployeeType(),
                o.getDepartment() != null ? o.getDepartment().getId() : null,
                o.getDepartment() != null ? o.getDepartment().getName() : null,
                o.getDesignation() != null ? o.getDesignation().getId() : null,
                o.getDesignation() != null ? o.getDesignation().getName() : null,
                o.getManager() != null ? o.getManager().getId() : null,
                o.getManager() != null ? (o.getManager().getFirstName() + " " + o.getManager().getLastName()).trim() : null,
                o.getJoinDate(),
                o.getOfferReleaseDate(),
                o.getProbationPeriodMonths(),
                o.getJoiningBonus(),
                o.getYearlyBonus(),
                o.getAnnualCtc(),
                o.getCurrency(),
                o.getBodyHtml(),
                o.getPdfGeneratedAt(),
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
