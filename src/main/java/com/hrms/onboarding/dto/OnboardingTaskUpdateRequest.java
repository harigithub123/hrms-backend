package com.hrms.onboarding.dto;

/**
 * Partial update: only non-null fields are applied. {@code done} and {@code status} are kept in sync when both are used.
 */
public record OnboardingTaskUpdateRequest(
        Boolean done,
        String status,
        String comment,
        String name
) {}
