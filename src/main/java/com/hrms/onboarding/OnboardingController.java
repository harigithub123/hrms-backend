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

    @PostMapping("/{caseId}/tasks")
    public OnboardingCaseDto addTask(
            @PathVariable Long caseId,
            @Valid @RequestBody OnboardingTaskCreateRequest req
    ) {
        return onboardingService.addTask(caseId, req);
    }

    @PatchMapping("/{caseId}/tasks/{taskId}")
    public OnboardingTaskDto updateTask(
            @PathVariable Long caseId,
            @PathVariable Long taskId,
            @Valid @RequestBody OnboardingTaskUpdateRequest req
    ) {
        return onboardingService.updateTask(caseId, taskId, req);
    }

    @PutMapping("/{caseId}/bank-details")
    public OnboardingCaseDto saveBankDetails(
            @PathVariable Long caseId,
            @Valid @RequestBody OnboardingBankDetailsUpsertRequest req
    ) {
        return onboardingService.saveBankDetails(caseId, req);
    }

    @PostMapping("/{id}/complete")
    public OnboardingCaseDto complete(@PathVariable Long id) {
        return onboardingService.complete(id);
    }
}
