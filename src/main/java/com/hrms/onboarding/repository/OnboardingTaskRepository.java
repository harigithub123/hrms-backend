package com.hrms.onboarding.repository;

import com.hrms.onboarding.entity.OnboardingTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OnboardingTaskRepository extends JpaRepository<OnboardingTask, Long> {
    List<OnboardingTask> findByOnboardingCaseIdOrderBySortOrderAsc(Long caseId);
}
