package com.hrms.compensation.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CompensationLineRequest(
        @NotNull Long componentId,
        @NotNull BigDecimal amount
) {}
