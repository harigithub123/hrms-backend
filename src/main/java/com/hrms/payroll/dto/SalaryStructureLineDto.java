package com.hrms.payroll.dto;

import com.hrms.payroll.SalaryComponentKind;
import com.hrms.payroll.entity.SalaryStructureLine;

import java.math.BigDecimal;

public record SalaryStructureLineDto(
        Long componentId,
        String componentCode,
        String componentName,
        SalaryComponentKind kind,
        BigDecimal amount
) {
    public static SalaryStructureLineDto from(SalaryStructureLine line) {
        var c = line.getComponent();
        String derivedCode = c.getName() != null
                ? c.getName().trim().toUpperCase().replaceAll("[^A-Z0-9]+", "_").replaceAll("^_+|_+$", "")
                : "";
        return new SalaryStructureLineDto(
                c.getId(),
                derivedCode,
                c.getName(),
                c.getKind(),
                line.getAmount()
        );
    }
}
