package com.hrms.compensation.dto;

import com.hrms.compensation.CompensationFrequency;
import com.hrms.compensation.entity.EmployeeCompensation;
import com.hrms.compensation.entity.EmployeeCompensationLine;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record CompensationDto(
        Long id,
        Long employeeId,
        String employeeName,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        String currency,
        BigDecimal annualCtc,
        BigDecimal annualBonus,
        BigDecimal joiningBonus,
        String notes,
        Instant createdAt,
        List<CompensationLineDto> lines
) {
    private static final String CODE_ANNUAL_BONUS = "ANNUAL_BONUS";
    private static final String CODE_JOINING_BONUS = "JOINING_BONUS";

    public static CompensationDto from(EmployeeCompensation c) {
        List<EmployeeCompensationLine> rawLines = c.getLines();
        BigDecimal annualBonus = sumAnnualBonus(rawLines);
        BigDecimal joiningBonus = sumJoiningBonus(rawLines);
        List<CompensationLineDto> lineDtos = rawLines.stream().map(CompensationLineDto::from).toList();
        String name = employeeDisplayName(c);
        return new CompensationDto(
                c.getId(),
                c.getEmployee().getId(),
                name,
                c.getEffectiveFrom(),
                c.getEffectiveTo(),
                c.getCurrency(),
                c.getAnnualCtc(),
                annualBonus,
                joiningBonus,
                c.getNotes(),
                c.getCreatedAt(),
                lineDtos
        );
    }

    private static String employeeDisplayName(EmployeeCompensation c) {
        var e = c.getEmployee();
        if (e == null) {
            return "";
        }
        String fn = e.getFirstName() != null ? e.getFirstName() : "";
        String ln = e.getLastName() != null ? e.getLastName() : "";
        return (fn + " " + ln).trim();
    }

    private static BigDecimal sumAnnualBonus(List<EmployeeCompensationLine> lines) {
        return lines.stream()
                .filter(l -> l.getFrequency() == CompensationFrequency.YEARLY
                        && CODE_ANNUAL_BONUS.equals(l.getComponent().getCode()))
                .map(EmployeeCompensationLine::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal sumJoiningBonus(List<EmployeeCompensationLine> lines) {
        return lines.stream()
                .filter(l -> l.getFrequency() == CompensationFrequency.ONE_TIME
                        && CODE_JOINING_BONUS.equals(l.getComponent().getCode()))
                .map(EmployeeCompensationLine::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
