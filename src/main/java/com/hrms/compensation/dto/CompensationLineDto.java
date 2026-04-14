package com.hrms.compensation.dto;

import com.hrms.compensation.entity.EmployeeCompensationLine;

import java.math.BigDecimal;

public record CompensationLineDto(
        Long id,
        Long componentId,
        String componentCode,
        String componentName,
        BigDecimal amount,
        com.hrms.compensation.CompensationFrequency frequency
) {
    public static CompensationLineDto from(EmployeeCompensationLine line) {
        String name = line.getComponent() != null ? line.getComponent().getName() : "";
        String derivedCode = name != null
                ? name.trim().toUpperCase().replaceAll("[^A-Z0-9]+", "_").replaceAll("^_+|_+$", "")
                : "";
        return new CompensationLineDto(
                line.getId(),
                line.getComponent().getId(),
                derivedCode,
                line.getComponent().getName(),
                line.getAmount(),
                line.getFrequency()
        );
    }
}
