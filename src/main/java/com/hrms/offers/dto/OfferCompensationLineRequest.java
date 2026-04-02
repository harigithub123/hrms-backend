package com.hrms.offers.dto;

import jakarta.validation.constraints.NotNull;

import com.hrms.compensation.CompensationFrequency;

import java.math.BigDecimal;

public record OfferCompensationLineRequest(
        @NotNull Long componentId,
        @NotNull BigDecimal amount,
        @NotNull CompensationFrequency frequency
) {}

