package com.hrms.compensation.dto;

import com.hrms.compensation.CompensationFrequency;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CompensationLineRequest(
        @NotNull Long componentId,
        @NotNull BigDecimal amount,
        @NotNull CompensationFrequency frequency,
        LocalDate payableOn
) {}
