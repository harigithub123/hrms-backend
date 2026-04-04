package com.hrms.onboarding.repository;

import com.hrms.onboarding.entity.OnboardingTaskAudit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface OnboardingTaskAuditRepository extends JpaRepository<OnboardingTaskAudit, Long> {
    List<OnboardingTaskAudit> findByTask_IdIn(Collection<Long> taskIds);
}
