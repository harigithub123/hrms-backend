package com.hrms.offers.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record OfferCreateRequest(
        @NotBlank @Size(max = 200) String candidateName,
        @Size(max = 255) String candidateEmail,
        @Size(max = 30) String candidateMobile,
        @NotNull
        @Size(max = 30) String employeeType,
        @NotNull
        Long departmentId,
        @NotNull
        Long designationId,
        @NotNull
        LocalDate joiningDate,
        @NotNull
        Integer probationPeriodMonths,
        BigDecimal annualCtc,
        List<OfferCompensationLineRequest> compensationLines
) {}

