package com.hrms.payroll.dto;

import com.hrms.payroll.entity.SalaryStructure;

import java.time.LocalDate;
import java.util.List;

public record SalaryStructureDto(
        Long id,
        Long employeeId,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        boolean isActive,
        String note,
        List<SalaryStructureLineDto> lines
) {
    public static SalaryStructureDto from(SalaryStructure s) {
        return new SalaryStructureDto(
                s.getId(),
                s.getEmployee().getId(),
                s.getEffectiveFrom(),
                s.getEffectiveTo(),
                s.isActive(),
                s.getNote(),
                s.getLines().stream().map(SalaryStructureLineDto::from).toList()
        );
    }
}
