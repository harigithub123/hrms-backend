package com.hrms.advance.dto;

import java.math.BigDecimal;

public record SalaryAdvanceApproveRequest(
        int recoveryMonths,
        BigDecimal recoveryAmountPerMonth
) {}
