package com.hrms.onboarding.dto;

import com.hrms.onboarding.entity.OnboardingTask;

public record OnboardingTaskDto(Long id, String label, boolean done, int sortOrder) {
    public static OnboardingTaskDto from(OnboardingTask t) {
        return new OnboardingTaskDto(t.getId(), t.getLabel(), t.isDone(), t.getSortOrder());
    }
}
