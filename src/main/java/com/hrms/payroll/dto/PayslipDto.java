package com.hrms.payroll.dto;

import com.hrms.payroll.entity.Payslip;

import java.math.BigDecimal;
import java.util.List;

public record PayslipDto(
        Long id,
        Long payRunId,
        Long employeeId,
        String employeeName,
        BigDecimal grossAmount,
        BigDecimal deductionAmount,
        BigDecimal netAmount,
        List<PayslipLineDto> lines
) {
    public static PayslipDto from(Payslip p) {
        String name = (p.getEmployee().getFirstName() + " " + p.getEmployee().getLastName()).trim();
        return new PayslipDto(
                p.getId(),
                p.getPayRun().getId(),
                p.getEmployee().getId(),
                name,
                p.getGrossAmount(),
                p.getDeductionAmount(),
                p.getNetAmount(),
                p.getLines().stream().map(PayslipLineDto::from).toList()
        );
    }
}
