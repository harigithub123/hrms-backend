package com.hrms.leave.dto;

import jakarta.validation.constraints.NotNull;

public record LeaveDecisionRequest(
        @NotNull Boolean approve,
        String comment
) {}
