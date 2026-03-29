package com.hrms.leave.dto;

import com.hrms.leave.LeaveBalanceAdjustmentKind;
import com.hrms.leave.entity.LeaveBalanceAdjustment;

import java.math.BigDecimal;
import java.time.Instant;

public record LeaveBalanceAdjustmentDto(
        Long id,
        Long employeeId,
        Long leaveTypeId,
        String leaveTypeCode,
        String leaveTypeName,
        int year,
        LeaveBalanceAdjustmentKind kind,
        BigDecimal deltaDays,
        String comment,
        Instant createdAt,
        String createdByUsername
) {
    public static LeaveBalanceAdjustmentDto from(LeaveBalanceAdjustment a) {
        var lb = a.getLeaveBalance();
        var lt = lb.getLeaveType();
        String by = a.getCreatedBy() != null ? a.getCreatedBy().getUsername() : null;
        return new LeaveBalanceAdjustmentDto(
                a.getId(),
                lb.getEmployee().getId(),
                lt.getId(),
                lt.getCode(),
                lt.getName(),
                lb.getYear(),
                a.getKind(),
                a.getDeltaDays(),
                a.getCommentText(),
                a.getCreatedAt(),
                by
        );
    }
}
