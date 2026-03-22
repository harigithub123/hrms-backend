package com.hrms.offers.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record JobOfferCreateRequest(
        Long templateId,
        @NotBlank @Size(max = 200) String candidateName,
        @Size(max = 255) String candidateEmail,
        Long departmentId,
        Long designationId,
        Long managerId,
        LocalDate joinDate,
        BigDecimal annualCtc,
        @Size(max = 10) String currency
) {}
