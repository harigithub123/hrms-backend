package com.hrms.onboarding.repository;

import com.hrms.onboarding.entity.OnboardingBankDetails;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OnboardingBankDetailsRepository extends JpaRepository<OnboardingBankDetails, Long> {
    Optional<OnboardingBankDetails> findByOnboardingCase_Id(Long caseId);
}
