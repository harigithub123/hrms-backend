package com.hrms.onboarding;

import com.hrms.onboarding.dto.OnboardingCaseDto;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/hr/separation-board")
@PreAuthorize("hasAnyRole('HR','ADMIN')")
public class SeparationBoardController {

    private final OnboardingService onboardingService;

    public SeparationBoardController(OnboardingService onboardingService) {
        this.onboardingService = onboardingService;
    }

    @GetMapping
    public List<OnboardingCaseDto> list() {
        return onboardingService.listSeparationBoard();
    }

    @PostMapping("/sync")
    public List<OnboardingCaseDto> sync() {
        return onboardingService.syncSeparationBoardCases();
    }
}
