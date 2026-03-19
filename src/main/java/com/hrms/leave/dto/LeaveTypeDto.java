package com.hrms.leave.dto;

import com.hrms.leave.entity.LeaveType;

import java.math.BigDecimal;

public record LeaveTypeDto(
        Long id,
        String name,
        String code,
        BigDecimal daysPerYear,
        boolean carryForward,
        boolean paid,
        boolean active
) {
    public static LeaveTypeDto from(LeaveType e) {
        return new LeaveTypeDto(
                e.getId(),
                e.getName(),
                e.getCode(),
                e.getDaysPerYear(),
                e.isCarryForward(),
                e.isPaid(),
                e.isActive()
        );
    }
}
