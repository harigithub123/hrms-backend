package com.hrms.onboarding.dto;

import com.hrms.onboarding.entity.OnboardingTaskAudit;

import java.time.Instant;

public record OnboardingTaskAuditDto(
        Long id,
        String action,
        String detail,
        Instant createdAt,
        String createdByUsername
) {
    public static OnboardingTaskAuditDto from(OnboardingTaskAudit a) {
        return new OnboardingTaskAuditDto(
                a.getId(),
                a.getAction(),
                a.getDetail(),
                a.getCreatedAt(),
                a.getCreatedByUsername()
        );
    }
}
