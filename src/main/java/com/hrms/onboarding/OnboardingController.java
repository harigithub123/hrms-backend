package com.hrms.onboarding;

import com.hrms.onboarding.dto.*;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/onboarding")
@PreAuthorize("hasAnyRole('HR','ADMIN')")
public class OnboardingController {

    private final OnboardingService onboardingService;

    public OnboardingController(OnboardingService onboardingService) {
        this.onboardingService = onboardingService;
    }

    @GetMapping
    public List<OnboardingCaseDto> list() {
        return onboardingService.list();
    }

    @GetMapping("/{id}")
    public OnboardingCaseDto get(@PathVariable Long id) {
        return onboardingService.get(id);
    }

    @PostMapping
    public OnboardingCaseDto create(@Valid @RequestBody OnboardingCreateRequest req) {
        return onboardingService.create(req);
    }

    @PatchMapping("/{id}/status")
    public OnboardingCaseDto status(@PathVariable Long id, @RequestParam String status) {
        return onboardingService.updateStatus(id, OnboardingStatus.valueOf(status));
    }

    @PatchMapping("/{caseId}/tasks/{taskId}")
    public OnboardingTaskDto updateTask(
            @PathVariable Long caseId,
            @PathVariable Long taskId,
            @Valid @RequestBody OnboardingTaskUpdateRequest req
    ) {
        return onboardingService.updateTask(caseId, taskId, req);
    }

    @PostMapping("/{id}/complete")
    public OnboardingCaseDto complete(@PathVariable Long id) {
        return onboardingService.complete(id);
    }
}
