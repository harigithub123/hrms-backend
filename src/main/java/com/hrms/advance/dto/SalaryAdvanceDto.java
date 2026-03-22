package com.hrms.advance.dto;

import com.hrms.advance.AdvanceStatus;
import com.hrms.advance.entity.SalaryAdvance;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record SalaryAdvanceDto(
        Long id,
        Long employeeId,
        String employeeName,
        BigDecimal amount,
        String currency,
        String reason,
        AdvanceStatus status,
        Instant requestedAt,
        Long approvedByUserId,
        Instant approvedAt,
        String rejectedReason,
        LocalDate payoutDate,
        Instant paidAt,
        int recoveryMonths,
        BigDecimal recoveryAmountPerMonth,
        BigDecimal outstandingBalance
) {
    public static SalaryAdvanceDto from(SalaryAdvance a) {
        String name = (a.getEmployee().getFirstName() + " " + a.getEmployee().getLastName()).trim();
        return new SalaryAdvanceDto(
                a.getId(),
                a.getEmployee().getId(),
                name,
                a.getAmount(),
                a.getCurrency(),
                a.getReason(),
                a.getStatus(),
                a.getRequestedAt(),
                a.getApprovedBy() != null ? a.getApprovedBy().getId() : null,
                a.getApprovedAt(),
                a.getRejectedReason(),
                a.getPayoutDate(),
                a.getPaidAt(),
                a.getRecoveryMonths(),
                a.getRecoveryAmountPerMonth(),
                a.getOutstandingBalance()
        );
    }
}
