package com.hrms.payroll.dto;

import com.hrms.payroll.entity.EmployeePayrollBankAudit;

import java.time.Instant;
import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record PayrollBankAuditDto(
        Long id,
        String action,
        String detail,
        Long createdByUserId,
        String createdByUsername,
        Instant createdAt,
        /** Effective-from date of the bank state after the change (parsed from audit detail), if present. */
        LocalDate effectiveFrom
) {
    private static final Pattern EFF_PATTERN = Pattern.compile("eff=(\\d{4}-\\d{2}-\\d{2})");

    public static PayrollBankAuditDto from(EmployeePayrollBankAudit a) {
        return new PayrollBankAuditDto(
                a.getId(),
                a.getAction(),
                a.getDetail(),
                a.getCreatedByUserId(),
                a.getCreatedByUsername(),
                a.getCreatedAt(),
                extractEffectiveFromNewState(a.getDetail())
        );
    }

    public static LocalDate extractEffectiveFromNewState(String detail) {
        if (detail == null || detail.isBlank()) {
            return null;
        }
        int arrow = detail.lastIndexOf(" -> ");
        String tail = arrow >= 0 ? detail.substring(arrow + 4) : detail;
        Matcher m = EFF_PATTERN.matcher(tail);
        LocalDate last = null;
        while (m.find()) {
            last = LocalDate.parse(m.group(1));
        }
        return last;
    }
}
