package com.hrms.payroll.dto;

import com.hrms.payroll.entity.EmployeeSalaryStructure;

import java.time.LocalDate;
import java.util.List;

public record SalaryStructureDto(
        Long id,
        Long employeeId,
        LocalDate effectiveFrom,
        String currency,
        String note,
        List<SalaryStructureLineDto> lines
) {
    public static SalaryStructureDto from(EmployeeSalaryStructure s) {
        return new SalaryStructureDto(
                s.getId(),
                s.getEmployee().getId(),
                s.getEffectiveFrom(),
                s.getCurrency(),
                s.getNote(),
                s.getLines().stream().map(SalaryStructureLineDto::from).toList()
        );
    }
}
