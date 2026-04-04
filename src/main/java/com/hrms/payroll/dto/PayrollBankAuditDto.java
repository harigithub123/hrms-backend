package com.hrms.payroll.dto;

import com.hrms.payroll.entity.EmployeePayrollBankAudit;

import java.time.Instant;

public record PayrollBankAuditDto(
        Long id,
        String action,
        String detail,
        Long createdByUserId,
        String createdByUsername,
        Instant createdAt
) {
    public static PayrollBankAuditDto from(EmployeePayrollBankAudit a) {
        return new PayrollBankAuditDto(
                a.getId(),
                a.getAction(),
                a.getDetail(),
                a.getCreatedByUserId(),
                a.getCreatedByUsername(),
                a.getCreatedAt()
        );
    }
}
