package com.hrms.offers.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record JobOfferCreateRequest(
        Long templateId,
        @NotBlank @Size(max = 200) String candidateName,
        @Size(max = 255) String candidateEmail,
        @Size(max = 30) String candidateMobile,
        @Size(max = 30) String employeeType,
        Long departmentId,
        Long designationId,
        Long managerId,
        LocalDate joinDate,
        LocalDate offerReleaseDate,
        Integer probationPeriodMonths,
        BigDecimal joiningBonus,
        BigDecimal yearlyBonus,
        BigDecimal annualCtc,
        @Size(max = 10) String currency,
        List<OfferCompensationLineRequest> compensationLines
) {}

