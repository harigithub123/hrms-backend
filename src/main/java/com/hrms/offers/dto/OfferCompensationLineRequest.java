package com.hrms.offers.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record OfferCompensationLineRequest(
        @NotNull Long componentId,
        @NotNull BigDecimal amount
) {}

