package com.hrms.compensation.dto;

import com.hrms.compensation.entity.EmployeeCompensationLine;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CompensationLineDto(
        Long id,
        Long componentId,
        String componentCode,
        String componentName,
        BigDecimal amount,
        com.hrms.compensation.CompensationFrequency frequency,
        LocalDate payableOn
) {
    public static CompensationLineDto from(EmployeeCompensationLine line) {
        return new CompensationLineDto(
                line.getId(),
                line.getComponent().getId(),
                line.getComponent().getCode(),
                line.getComponent().getName(),
                line.getAmount(),
                line.getFrequency(),
                line.getPayableOn()
        );
    }
}
