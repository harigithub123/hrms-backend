package com.hrms.compensation.dto;

import com.hrms.compensation.entity.EmployeeCompensation;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record CompensationDto(
        Long id,
        Long employeeId,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        String currency,
        BigDecimal annualCtc,
        String notes,
        Instant createdAt,
        List<CompensationLineDto> lines
) {
    public static CompensationDto from(EmployeeCompensation c) {
        return new CompensationDto(
                c.getId(),
                c.getEmployee().getId(),
                c.getEffectiveFrom(),
                c.getEffectiveTo(),
                c.getCurrency(),
                c.getAnnualCtc(),
                c.getNotes(),
                c.getCreatedAt(),
                c.getLines().stream().map(CompensationLineDto::from).toList()
        );
    }
}
