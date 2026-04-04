package com.hrms.onboarding.repository;

import com.hrms.onboarding.entity.OnboardingTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OnboardingTaskRepository extends JpaRepository<OnboardingTask, Long> {
    List<OnboardingTask> findByOnboardingCaseIdOrderBySortOrderAsc(Long caseId);

    long countByOnboardingCaseId(Long onboardingCaseId);

    @Query("SELECT COALESCE(MAX(t.sortOrder), -1) FROM OnboardingTask t WHERE t.onboardingCase.id = :caseId")
    int findMaxSortOrderForCase(@Param("caseId") Long caseId);
}
