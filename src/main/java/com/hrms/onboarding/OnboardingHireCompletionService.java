package com.hrms.onboarding;

import com.hrms.auth.EmployeeAccountService;
import com.hrms.auth.entity.User;
import com.hrms.compensation.CompensationFrequency;
import com.hrms.compensation.entity.EmployeeCompensation;
import com.hrms.compensation.entity.EmployeeCompensationLine;
import com.hrms.compensation.repository.EmployeeCompensationRepository;
import com.hrms.leave.entity.LeaveBalance;
import com.hrms.leave.entity.LeaveType;
import com.hrms.leave.repository.LeaveBalanceRepository;
import com.hrms.leave.repository.LeaveTypeRepository;
import com.hrms.offers.entity.JobOffer;
import com.hrms.offers.entity.OfferCompensation;
import com.hrms.offers.entity.OfferCompensationLine;
import com.hrms.offers.repository.JobOfferRepository;
import com.hrms.offers.repository.OfferCompensationRepository;
import com.hrms.onboarding.entity.OnboardingCase;
import com.hrms.onboarding.repository.OnboardingCaseRepository;
import com.hrms.payroll.EmployeePayrollBankService;
import com.hrms.org.entity.Employee;
import com.hrms.org.repository.EmployeeRepository;
import com.hrms.security.CurrentUserService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;

/**
 * Completes onboarding by creating the employee record, provisioning access, payroll bank setup,
 * offer linkage, compensation copy, and initial leave balances.
 */
@Service
public class OnboardingHireCompletionService {

    private final OnboardingCaseRepository caseRepository;
    private final EmployeeRepository employeeRepository;
    private final JobOfferRepository jobOfferRepository;
    private final EmployeeAccountService employeeAccountService;
    private final OfferCompensationRepository offerCompensationRepository;
    private final EmployeeCompensationRepository employeeCompensationRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final EmployeePayrollBankService employeePayrollBankService;
    private final CurrentUserService currentUserService;

    public OnboardingHireCompletionService(
            OnboardingCaseRepository caseRepository,
            EmployeeRepository employeeRepository,
            JobOfferRepository jobOfferRepository,
            EmployeeAccountService employeeAccountService,
            OfferCompensationRepository offerCompensationRepository,
            EmployeeCompensationRepository employeeCompensationRepository,
            LeaveTypeRepository leaveTypeRepository,
            LeaveBalanceRepository leaveBalanceRepository,
            EmployeePayrollBankService employeePayrollBankService,
            CurrentUserService currentUserService
    ) {
        this.caseRepository = caseRepository;
        this.employeeRepository = employeeRepository;
        this.jobOfferRepository = jobOfferRepository;
        this.employeeAccountService = employeeAccountService;
        this.offerCompensationRepository = offerCompensationRepository;
        this.employeeCompensationRepository = employeeCompensationRepository;
        this.leaveTypeRepository = leaveTypeRepository;
        this.leaveBalanceRepository = leaveBalanceRepository;
        this.employeePayrollBankService = employeePayrollBankService;
        this.currentUserService = currentUserService;
    }

    /**
     * Creates and links an employee for the given case (must not already be completed or have an employee).
     */
    public void completeHire(OnboardingCase c) {
        if (c.getStatus() == OnboardingStatus.COMPLETED) {
            throw new IllegalArgumentException("Already completed");
        }
        if (c.getEmployee() != null) {
            throw new IllegalArgumentException("Employee already linked");
        }

        Employee e = new Employee();
        e.setFirstName(c.getCandidateFirstName());
        e.setLastName(c.getCandidateLastName());
        e.setEmail(c.getCandidateEmail());
        e.setDepartment(c.getDepartment());
        e.setDesignation(c.getDesignation());
        e.setJoinedAt(c.getJoinDate());
        JobOffer linkedOffer = null;
        if (c.getOffer() != null) {
            linkedOffer = jobOfferRepository.findById(c.getOffer().getId()).orElse(null);
            if (linkedOffer != null) {
                if (linkedOffer.getCandidateEmail() != null && !linkedOffer.getCandidateEmail().isBlank()) {
                    e.setEmail(linkedOffer.getCandidateEmail());
                }
                if (linkedOffer.getCandidateMobile() != null && !linkedOffer.getCandidateMobile().isBlank()) {
                    e.setMobileNumber(linkedOffer.getCandidateMobile());
                }
            }
        }
        final Employee saved = employeeRepository.save(e);
        employeeAccountService.provisionUserForNewEmployee(saved);

        c.setEmployee(saved);
        c.setStatus(OnboardingStatus.COMPLETED);
        caseRepository.save(c);

        User actor = currentUserService.requireCurrentUser();
        employeePayrollBankService.ensureFromOnboardingAfterHire(c.getId(), saved.getId(), actor);

        if (linkedOffer != null) {
            linkedOffer.setEmployee(saved);
            jobOfferRepository.save(linkedOffer);
            copyOfferCompensationToEmployee(linkedOffer, saved, c.getJoinDate());
        }

        final int year = c.getJoinDate() != null ? c.getJoinDate().getYear() : Year.now().getValue();
        for (LeaveType lt : leaveTypeRepository.findByActiveTrueOrderByNameAsc()) {
            LeaveBalance b = leaveBalanceRepository
                    .findByEmployeeIdAndLeaveTypeIdAndYear(saved.getId(), lt.getId(), year)
                    .orElseGet(() -> {
                        LeaveBalance nb = new LeaveBalance();
                        nb.setEmployee(saved);
                        nb.setLeaveType(lt);
                        nb.setYear(year);
                        nb.setUsedDays(BigDecimal.ZERO);
                        return nb;
                    });
            b.setAllocatedDays(lt.getDaysPerYear());
            leaveBalanceRepository.save(b);
        }
    }

    private void copyOfferCompensationToEmployee(JobOffer offer, Employee employee, LocalDate effectiveFrom) {
        OfferCompensation offerComp = offerCompensationRepository.findByOfferId(offer.getId()).orElse(null);
        if (offerComp == null || offerComp.getOfferCompensationLine() == null
                || offerComp.getOfferCompensationLine().isEmpty()) {
            return;
        }

        EmployeeCompensation comp = new EmployeeCompensation();
        comp.setEmployee(employee);
        comp.setEffectiveFrom(effectiveFrom);
        comp.setCurrency(offerComp.getCurrency() != null ? offerComp.getCurrency()
                : (offer.getCurrency() != null ? offer.getCurrency() : "INR"));
        comp.setNotes("Created from offer #" + offer.getId());

        for (OfferCompensationLine ol : offerComp.getOfferCompensationLine()) {
            EmployeeCompensationLine nl = new EmployeeCompensationLine();
            nl.setCompensation(comp);
            nl.setComponent(ol.getComponent());
            nl.setAmount(ol.getAmount() != null ? ol.getAmount() : BigDecimal.ZERO);
            CompensationFrequency freq = ol.getFrequency() != null ? ol.getFrequency() : CompensationFrequency.MONTHLY;
            nl.setFrequency(freq);
            if (freq == CompensationFrequency.ONE_TIME) {
                nl.setPayableOn(effectiveFrom);
            }
            comp.getLines().add(nl);
        }
        comp.setAnnualCtc(comp.calculateAnnualCtc());
        employeeCompensationRepository.save(comp);
    }
}
