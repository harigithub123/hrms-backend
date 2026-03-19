package com.hrms.auth;

import java.util.List;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        long expiresInSeconds,
        String tokenType,
        UserInfo user
) {
    public static final String TOKEN_TYPE = "Bearer";

    public record UserInfo(
            Long id,
            String username,
            String email,
            List<String> roles,
            Long employeeId,
            /** Employees with this user as manager (for team leave / approvals UI). */
            int directReportCount
    ) {}
}
