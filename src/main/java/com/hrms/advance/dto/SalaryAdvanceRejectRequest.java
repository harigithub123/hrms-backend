package com.hrms.advance.dto;

import jakarta.validation.constraints.Size;

public record SalaryAdvanceRejectRequest(@Size(max = 1000) String reason) {}
