package com.hrms.payroll.dto;

import com.hrms.payroll.SalaryComponentKind;
import com.hrms.payroll.entity.PayrollFixedComponentAmount;

import java.math.BigDecimal;

public record PayrollFixedComponentDto(
        Long id,
        Long componentId,
        String componentCode,
        String componentName,
        SalaryComponentKind kind,
        BigDecimal monthlyAmount
) {
    public static PayrollFixedComponentDto from(PayrollFixedComponentAmount row) {
        var c = row.getSalaryComponent();
        return new PayrollFixedComponentDto(
                row.getId(),
                c.getId(),
                c.getCode(),
                c.getName(),
                c.getKind(),
                row.getMonthlyAmount()
        );
    }
}
