package com.hrms.auth;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record UpdateUserRolesRequest(
        @NotNull(message = "roles is required") List<String> roles
) {}
