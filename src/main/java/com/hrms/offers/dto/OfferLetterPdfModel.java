package com.hrms.offers.dto;

import com.hrms.offers.EmployeeType;
import com.hrms.offers.OfferPdfService;
import com.hrms.offers.entity.JobOffer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record OfferLetterPdfModel(
        EmployeeType employeeType,
        String employeeName,
        String personalEmail,
        String mobile,
        LocalDate joiningDate,
        LocalDate offerReleaseDate,
        int probationMonths,
        String designation,
        String department,
        BigDecimal annualCtc,
        List<OfferPdfService.OfferCompLine> compensationLines
) {
    public static OfferLetterPdfModel from(JobOffer o, List<OfferPdfService.OfferCompLine> lines) {
        return new OfferLetterPdfModel(
                o.getEmployeeType(),
                o.getCandidateName(),
                o.getCandidateEmail(),
                o.getCandidateMobile(),
                o.getJoiningDate(),
                o.getOfferReleaseDate(),
                o.getProbationPeriodMonths() != null ? o.getProbationPeriodMonths() : 0,
                o.getDesignation() != null ? o.getDesignation().getName() : "—",
                o.getDepartment() != null ? o.getDepartment().getName() : "—",
                o.getAnnualCtc(),
                lines
        );
    }
}
