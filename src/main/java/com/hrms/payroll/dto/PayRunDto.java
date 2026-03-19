package com.hrms.payroll.dto;

import com.hrms.payroll.PayRunStatus;
import com.hrms.payroll.entity.PayRun;

import java.time.LocalDate;

public record PayRunDto(
        Long id,
        LocalDate periodStart,
        LocalDate periodEnd,
        PayRunStatus status
) {
    public static PayRunDto from(PayRun r) {
        return new PayRunDto(r.getId(), r.getPeriodStart(), r.getPeriodEnd(), r.getStatus());
    }
}
