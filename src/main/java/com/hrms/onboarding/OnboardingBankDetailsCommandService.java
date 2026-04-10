package com.hrms.onboarding;

import com.hrms.auth.entity.User;
import com.hrms.onboarding.dto.OnboardingBankDetailsDto;
import com.hrms.onboarding.dto.OnboardingBankDetailsUpsertRequest;
import com.hrms.onboarding.entity.OnboardingBankDetails;
import com.hrms.onboarding.entity.OnboardingCase;
import com.hrms.onboarding.repository.OnboardingBankDetailsRepository;
import com.hrms.onboarding.repository.OnboardingCaseRepository;
import com.hrms.payroll.EmployeePayrollBankService;
import com.hrms.security.CurrentUserService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Persists onboarding bank details and syncs to payroll when an employee is already linked.
 */
@Service
public class OnboardingBankDetailsCommandService {

    private final OnboardingCaseRepository caseRepository;
    private final OnboardingBankDetailsRepository bankDetailsRepository;
    private final EmployeePayrollBankService employeePayrollBankService;
    private final CurrentUserService currentUserService;

    public OnboardingBankDetailsCommandService(
            OnboardingCaseRepository caseRepository,
            OnboardingBankDetailsRepository bankDetailsRepository,
            EmployeePayrollBankService employeePayrollBankService,
            CurrentUserService currentUserService
    ) {
        this.caseRepository = caseRepository;
        this.bankDetailsRepository = bankDetailsRepository;
        this.employeePayrollBankService = employeePayrollBankService;
        this.currentUserService = currentUserService;
    }

    public Optional<OnboardingBankDetailsDto> findForCase(long caseId) {
        return bankDetailsRepository.findByOnboardingCase_Id(caseId).map(OnboardingBankDetailsDto::from);
    }

    public void saveBankDetails(Long caseId, OnboardingBankDetailsUpsertRequest req) {
        OnboardingCase c = caseRepository.findById(caseId)
                .orElseThrow(() -> new IllegalArgumentException("Onboarding case not found: " + caseId));
        ensureBankDetailsEditable(c);
        OnboardingBankAccountType accountType;
        try {
            accountType = OnboardingBankAccountType.valueOf(req.accountType().trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("accountType must be SAVINGS or CURRENT");
        }
        OnboardingBankDetails b = bankDetailsRepository.findByOnboardingCase_Id(caseId).orElseGet(() -> {
            OnboardingBankDetails nb = new OnboardingBankDetails();
            nb.setOnboardingCase(c);
            return nb;
        });
        b.setAccountHolderName(req.accountHolderName().trim());
        b.setBankName(req.bankName().trim());
        b.setBranch(req.branch() != null && !req.branch().isBlank() ? req.branch().trim() : null);
        b.setAccountNumber(req.accountNumber().trim());
        b.setIfscCode(req.ifscCode().trim().toUpperCase());
        b.setAccountType(accountType);
        b.setNotes(req.notes() != null && !req.notes().isBlank() ? req.notes().trim() : null);
        bankDetailsRepository.save(b);
        User u = currentUserService.requireCurrentUser();
        LocalDate eff = req.effectiveFrom() != null ? req.effectiveFrom() : LocalDate.now();
        if (c.getEmployee() != null) {
            employeePayrollBankService.syncFromOnboardingCaseBank(c, b, eff, u);
        }
    }

    private void ensureBankDetailsEditable(OnboardingCase c) {
        if (c.getStatus() == OnboardingStatus.CANCELLED) {
            throw new IllegalArgumentException("Cannot modify bank details for a cancelled onboarding case");
        }
    }
}
