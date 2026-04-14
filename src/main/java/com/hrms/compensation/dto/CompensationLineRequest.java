package com.hrms.compensation.dto;

import com.hrms.compensation.CompensationFrequency;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CompensationLineRequest(
        @NotNull Long componentId,
        @NotNull BigDecimal amount,
        @NotNull CompensationFrequency frequency
) {}
