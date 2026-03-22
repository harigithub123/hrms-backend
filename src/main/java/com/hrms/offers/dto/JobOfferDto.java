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
        OfferStatus status,
        Long departmentId,
        String departmentName,
        Long designationId,
        String designationName,
        Long managerId,
        String managerName,
        LocalDate joinDate,
        BigDecimal annualCtc,
        String currency,
        String bodyHtml,
        Instant pdfGeneratedAt,
        Instant createdAt
) {
    public static JobOfferDto from(JobOffer o) {
        return new JobOfferDto(
                o.getId(),
                o.getTemplate() != null ? o.getTemplate().getId() : null,
                o.getCandidateName(),
                o.getCandidateEmail(),
                o.getStatus(),
                o.getDepartment() != null ? o.getDepartment().getId() : null,
                o.getDepartment() != null ? o.getDepartment().getName() : null,
                o.getDesignation() != null ? o.getDesignation().getId() : null,
                o.getDesignation() != null ? o.getDesignation().getName() : null,
                o.getManager() != null ? o.getManager().getId() : null,
                o.getManager() != null ? (o.getManager().getFirstName() + " " + o.getManager().getLastName()).trim() : null,
                o.getJoinDate(),
                o.getAnnualCtc(),
                o.getCurrency(),
                o.getBodyHtml(),
                o.getPdfGeneratedAt(),
                o.getCreatedAt()
        );
    }
}
