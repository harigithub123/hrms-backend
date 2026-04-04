package com.hrms.onboarding.dto;

import com.hrms.onboarding.entity.OnboardingTask;

import java.util.List;

public record OnboardingTaskDto(
        Long id,
        String name,
        String status,
        boolean done,
        String comment,
        int sortOrder,
        List<OnboardingTaskAuditDto> auditHistory
) {
    public static OnboardingTaskDto from(OnboardingTask t, List<OnboardingTaskAuditDto> audits) {
        return new OnboardingTaskDto(
                t.getId(),
                t.getLabel(),
                t.getStatus().name(),
                t.isDone(),
                t.getCommentText(),
                t.getSortOrder(),
                audits
        );
    }
}
