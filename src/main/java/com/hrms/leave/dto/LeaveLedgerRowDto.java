package com.hrms.leave.dto;

import java.math.BigDecimal;

/**
 * One row in the leave ledger report (chronological, per leave type running balance).
 */
public record LeaveLedgerRowDto(
        String entryDate,
        String leaveTypeCode,
        String leaveTypeName,
        /** OPENING, ALLOCATED, CARRY_FORWARD, LAPSE, LEAVE_TAKEN */
        String action,
        /** Magnitude of days (positive); null for opening. */
        BigDecimal days,
        /** Available balance (allocated + carry-forward − used) after this entry. */
        BigDecimal balanceAfter,
        /** Comment, leave reason, or short note. */
        String details
) {}
