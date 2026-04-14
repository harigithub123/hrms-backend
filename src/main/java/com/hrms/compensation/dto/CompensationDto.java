package com.hrms.compensation.dto;

import com.hrms.compensation.CompensationFrequency;
import com.hrms.compensation.entity.EmployeeCompensation;
import com.hrms.compensation.entity.EmployeeCompensationLine;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record CompensationDto(
        Long id,
        Long employeeId,
        String employeeName,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        BigDecimal annualCtc,
        BigDecimal annualBonus,
        BigDecimal joiningBonus,
        String notes,
        Instant createdAt,
        List<CompensationLineDto> lines
) {

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
                c.getAnnualCtc(),
                annualBonus,
                joiningBonus,
                c.getNotes(),
                c.getCreatedAt(),
                lineDtos
        );
    }

    public BigDecimal calculateAnnualCtc() {
        return lines.stream()
                .map(this::toAnnualAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal toAnnualAmount(CompensationLineDto line) {
        BigDecimal amount = safe(line.amount());

        return switch (line.frequency()) {
            case MONTHLY -> amount.multiply(BigDecimal.valueOf(12));
            case YEARLY, ONE_TIME -> amount;
        };
    }

    private BigDecimal safe(BigDecimal val) {
        return val == null ? BigDecimal.ZERO : val;
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
                .filter(l -> l.getFrequency() == CompensationFrequency.YEARLY)
                .map(EmployeeCompensationLine::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal sumJoiningBonus(List<EmployeeCompensationLine> lines) {
        return lines.stream()
                .filter(l -> l.getFrequency() == CompensationFrequency.ONE_TIME)
                .map(EmployeeCompensationLine::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
