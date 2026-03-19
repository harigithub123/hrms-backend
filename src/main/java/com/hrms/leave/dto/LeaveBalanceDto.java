package com.hrms.leave.dto;

import com.hrms.leave.entity.LeaveBalance;

import java.math.BigDecimal;

public record LeaveBalanceDto(
        Long id,
        Long employeeId,
        Long leaveTypeId,
        String leaveTypeName,
        String leaveTypeCode,
        int year,
        BigDecimal allocatedDays,
        BigDecimal usedDays
) {
    public static LeaveBalanceDto from(LeaveBalance b) {
        return new LeaveBalanceDto(
                b.getId(),
                b.getEmployee().getId(),
                b.getLeaveType().getId(),
                b.getLeaveType().getName(),
                b.getLeaveType().getCode(),
                b.getYear(),
                b.getAllocatedDays(),
                b.getUsedDays()
        );
    }
}
