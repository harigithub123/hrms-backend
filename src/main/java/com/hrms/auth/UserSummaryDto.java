package com.hrms.auth;

import java.util.List;

public record UserSummaryDto(Long id, String username, Long employeeId, List<String> roles) {}
