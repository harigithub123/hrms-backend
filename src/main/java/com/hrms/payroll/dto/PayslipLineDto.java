package com.hrms.payroll.dto;

import com.hrms.payroll.SalaryComponentKind;
import com.hrms.payroll.entity.PayslipLine;

import java.math.BigDecimal;

public record PayslipLineDto(
        Long componentId,
        String componentCode,
        String componentName,
        SalaryComponentKind kind,
        BigDecimal amount
) {
    public static PayslipLineDto from(PayslipLine line) {
        return new PayslipLineDto(
                line.getComponent() != null ? line.getComponent().getId() : null,
                line.getComponentCode(),
                line.getComponentName(),
                line.getKind(),
                line.getAmount()
        );
    }
}
