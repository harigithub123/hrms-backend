package com.hrms.payroll.dto;

import com.hrms.payroll.PayRunStatus;
import com.hrms.payroll.entity.PayRun;

public record PayRunDto(
        Long id,
        int year,
        int month,
        PayRunStatus status
) {
    public static PayRunDto from(PayRun r) {
        return new PayRunDto(r.getId(), r.getPayYear(), r.getPayMonth(), r.getStatus());
    }
}
