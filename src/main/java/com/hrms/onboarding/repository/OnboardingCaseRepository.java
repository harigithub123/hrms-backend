package com.hrms.onboarding.repository;

import com.hrms.onboarding.entity.OnboardingCase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OnboardingCaseRepository extends JpaRepository<OnboardingCase, Long> {
    List<OnboardingCase> findAllByOrderByIdDesc();

    Optional<OnboardingCase> findFirstByOffer_IdOrderByIdDesc(Long offerId);
}
