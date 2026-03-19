package com.hrms.payroll.dto;

import com.hrms.payroll.SalaryComponentKind;
import com.hrms.payroll.entity.EmployeeSalaryStructureLine;

import java.math.BigDecimal;

public record SalaryStructureLineDto(
        Long componentId,
        String componentCode,
        String componentName,
        SalaryComponentKind kind,
        BigDecimal amount
) {
    public static SalaryStructureLineDto from(EmployeeSalaryStructureLine line) {
        var c = line.getComponent();
        return new SalaryStructureLineDto(
                c.getId(),
                c.getCode(),
                c.getName(),
                c.getKind(),
                line.getAmount()
        );
    }
}
