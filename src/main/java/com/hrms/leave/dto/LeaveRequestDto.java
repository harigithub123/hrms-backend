package com.hrms.leave.dto;

import com.hrms.leave.LeaveRequestStatus;
import com.hrms.leave.entity.LeaveRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record LeaveRequestDto(
        Long id,
        Long employeeId,
        String employeeName,
        Long leaveTypeId,
        String leaveTypeName,
        String leaveTypeCode,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal totalDays,
        String reason,
        LeaveRequestStatus status,
        Instant requestedAt,
        Instant decidedAt,
        Long decidedByUserId,
        String decisionComment
) {
    public static LeaveRequestDto from(LeaveRequest r) {
        String name = (r.getEmployee().getFirstName() + " " + r.getEmployee().getLastName()).trim();
        return new LeaveRequestDto(
                r.getId(),
                r.getEmployee().getId(),
                name,
                r.getLeaveType().getId(),
                r.getLeaveType().getName(),
                r.getLeaveType().getCode(),
                r.getStartDate(),
                r.getEndDate(),
                r.getTotalDays(),
                r.getReason(),
                r.getStatus(),
                r.getRequestedAt(),
                r.getDecidedAt(),
                r.getDecidedBy() != null ? r.getDecidedBy().getId() : null,
                r.getDecisionComment()
        );
    }
}
