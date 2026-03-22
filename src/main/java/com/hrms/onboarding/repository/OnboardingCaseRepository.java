package com.hrms.onboarding.repository;

import com.hrms.onboarding.entity.OnboardingCase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OnboardingCaseRepository extends JpaRepository<OnboardingCase, Long> {
    List<OnboardingCase> findAllByOrderByIdDesc();
}
