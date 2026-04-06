package com.hrms.payroll;

import com.hrms.auth.entity.User;
import com.hrms.onboarding.OnboardingBankAccountType;
import com.hrms.onboarding.dto.EmployeePayrollBankContextDto;
import com.hrms.onboarding.dto.OnboardingBankDetailsDto;
import com.hrms.onboarding.dto.OnboardingBankDetailsUpsertRequest;
import com.hrms.onboarding.entity.OnboardingBankDetails;
import com.hrms.onboarding.entity.OnboardingCase;
import com.hrms.onboarding.repository.OnboardingBankDetailsRepository;
import com.hrms.onboarding.repository.OnboardingCaseRepository;
import com.hrms.org.entity.Employee;
import com.hrms.org.repository.EmployeeRepository;
import com.hrms.payroll.dto.EmployeePayrollBankSummaryDto;
import com.hrms.payroll.dto.PayrollBankAuditDto;
import com.hrms.payroll.entity.EmployeePayrollBank;
import com.hrms.payroll.entity.EmployeePayrollBankAudit;
import com.hrms.payroll.repository.EmployeePayrollBankAuditRepository;
import com.hrms.payroll.repository.EmployeePayrollBankRepository;
import com.hrms.security.CurrentUserService;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class EmployeePayrollBankService {

    private final EmployeeRepository employeeRepository;
    private final OnboardingCaseRepository caseRepository;
    private final OnboardingBankDetailsRepository onboardingBankDetailsRepository;
    private final EmployeePayrollBankRepository payrollBankRepository;
    private final EmployeePayrollBankAuditRepository auditRepository;
    private final CurrentUserService currentUserService;

    public EmployeePayrollBankService(
            EmployeeRepository employeeRepository,
            OnboardingCaseRepository caseRepository,
            OnboardingBankDetailsRepository onboardingBankDetailsRepository,
            EmployeePayrollBankRepository payrollBankRepository,
            EmployeePayrollBankAuditRepository auditRepository,
            CurrentUserService currentUserService
    ) {
        this.employeeRepository = employeeRepository;
        this.caseRepository = caseRepository;
        this.onboardingBankDetailsRepository = onboardingBankDetailsRepository;
        this.payrollBankRepository = payrollBankRepository;
        this.auditRepository = auditRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public EmployeePayrollBankContextDto getContextForHr(Long employeeId) {
        if (!employeeRepository.existsById(employeeId)) {
            throw new IllegalArgumentException("Employee not found: " + employeeId);
        }
        return buildContext(employeeId);
    }

    /**
     * HR adds a new payroll-bank version: each save must use an effective date on or after today,
     * strictly after the current row when one exists, and never reuse a date already recorded in audit history.
     * Existing active details are not edited in place; onboarding sync paths use other methods.
     */
    @Transactional
    public EmployeePayrollBankContextDto upsertForHr(Long employeeId, OnboardingBankDetailsUpsertRequest req) {
        User actor = currentUserService.requireCurrentUser();
        Employee emp = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + employeeId));
        LocalDate eff = req.effectiveFrom() != null ? req.effectiveFrom() : LocalDate.now();
        OnboardingBankAccountType accountType = parseAccountType(req.accountType());

        LocalDate today = LocalDate.now();
        if (eff.isBefore(today)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Effective date cannot be in the past; backdated bank changes can conflict with payslips already processed.");
        }

        Optional<EmployeePayrollBank> before = payrollBankRepository.findByEmployee_Id(employeeId);
        assertHrEffectiveDateAfterCurrentActive(before, eff);
        assertEffectiveDateNotAlreadyUsedInHistory(employeeId, eff);

        EmployeePayrollBank row = before.orElseGet(() -> {
            EmployeePayrollBank n = new EmployeePayrollBank();
            n.setEmployee(emp);
            return n;
        });

        String detailBefore = before.map(this::auditSnapshot).orElse("(none)");
        applyUpsertToEntity(req, accountType, eff, row);
        payrollBankRepository.save(row);

        String detailAfter = auditSnapshot(row);
        String action = before.isEmpty() ? "HR_CREATED" : "HR_ADDED";
        recordAudit(emp, action, detailBefore + " -> " + detailAfter, actor);

        mirrorToOnboardingCaseIfPresent(emp, row);
        return buildContext(employeeId);
    }

    /**
     * Called after onboarding saves bank on a case that already has an employee linked.
     */
    @Transactional
    public void syncFromOnboardingCaseBank(OnboardingCase c, OnboardingBankDetails b, LocalDate effectiveFrom, User actor) {
        if (c.getEmployee() == null) {
            return;
        }
        Employee emp = c.getEmployee();
        LocalDate eff = effectiveFrom != null ? effectiveFrom : LocalDate.now();
        Optional<EmployeePayrollBank> before = payrollBankRepository.findByEmployee_Id(emp.getId());
        EmployeePayrollBank row = before.orElseGet(() -> {
            EmployeePayrollBank n = new EmployeePayrollBank();
            n.setEmployee(emp);
            return n;
        });
        copyFromOnboardingEntity(b, row, eff);
        payrollBankRepository.save(row);
        String detailBefore = before.map(this::auditSnapshot).orElse("(none)");
        recordAudit(emp, "SYNCED_FROM_ONBOARDING", detailBefore + " -> " + auditSnapshot(row), actor);
    }

    @Transactional
    public void ensureFromOnboardingAfterHire(Long caseId, Long employeeId, User actor) {
        OnboardingBankDetails obd = onboardingBankDetailsRepository.findByOnboardingCase_Id(caseId).orElse(null);
        if (obd == null) {
            return;
        }
        Employee emp = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + employeeId));
        LocalDate eff = emp.getJoinedAt() != null ? emp.getJoinedAt() : LocalDate.now();
        Optional<EmployeePayrollBank> before = payrollBankRepository.findByEmployee_Id(employeeId);
        EmployeePayrollBank row = before.orElseGet(() -> {
            EmployeePayrollBank n = new EmployeePayrollBank();
            n.setEmployee(emp);
            return n;
        });
        copyFromOnboardingEntity(obd, row, eff);
        payrollBankRepository.save(row);
        String detailBefore = before.map(this::auditSnapshot).orElse("(none)");
        recordAudit(emp, "HIRING_SYNC", detailBefore + " -> " + auditSnapshot(row), actor);
    }

    @Transactional(readOnly = true)
    public List<PayrollBankAuditDto> listAuditsForHr(Long employeeId) {
        if (!employeeRepository.existsById(employeeId)) {
            throw new IllegalArgumentException("Employee not found: " + employeeId);
        }
        return auditRepository.findByEmployee_IdOrderByCreatedAtDesc(employeeId).stream()
                .map(PayrollBankAuditDto::from)
                .sorted(Comparator
                        .comparing(PayrollBankAuditDto::effectiveFrom, Comparator.nullsLast(Comparator.naturalOrder()))
                        .reversed()
                        .thenComparing(PayrollBankAuditDto::createdAt, Comparator.reverseOrder()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EmployeePayrollBankSummaryDto> listHrEmployeeSummaries() {
        return employeeRepository.findAll(Sort.by(Sort.Order.asc("firstName"), Sort.Order.asc("lastName"))).stream()
                .map(e -> EmployeePayrollBankSummaryDto.of(e, resolveDisplayBank(e.getId())))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<EmployeePayrollBank> findForEmployee(Long employeeId) {
        return payrollBankRepository.findByEmployee_Id(employeeId);
    }

    private EmployeePayrollBankContextDto buildContext(Long employeeId) {
        Optional<OnboardingCase> oc = caseRepository.findFirstByEmployee_IdOrderByIdDesc(employeeId);
        boolean linked = oc.isPresent();
        Long caseId = oc.map(OnboardingCase::getId).orElse(null);
        OnboardingBankDetailsDto bank = resolveDisplayBank(employeeId);
        return new EmployeePayrollBankContextDto(linked, caseId, bank);
    }

    private OnboardingBankDetailsDto resolveDisplayBank(Long employeeId) {
        return payrollBankRepository.findByEmployee_Id(employeeId)
                .map(OnboardingBankDetailsDto::fromEmployeePayrollBank)
                .or(() -> caseRepository.findFirstByEmployee_IdOrderByIdDesc(employeeId)
                        .flatMap(c -> onboardingBankDetailsRepository.findByOnboardingCase_Id(c.getId()))
                        .map(OnboardingBankDetailsDto::from))
                .orElse(null);
    }

    private void mirrorToOnboardingCaseIfPresent(Employee emp, EmployeePayrollBank row) {
        caseRepository.findFirstByEmployee_IdOrderByIdDesc(emp.getId()).ifPresent(c -> {
            OnboardingBankDetails obd = onboardingBankDetailsRepository.findByOnboardingCase_Id(c.getId()).orElseGet(() -> {
                OnboardingBankDetails n = new OnboardingBankDetails();
                n.setOnboardingCase(c);
                return n;
            });
            obd.setAccountHolderName(row.getAccountHolderName());
            obd.setBankName(row.getBankName());
            obd.setBranch(row.getBranch());
            obd.setAccountNumber(row.getAccountNumber());
            obd.setIfscCode(row.getIfscCode());
            obd.setAccountType(row.getAccountType());
            obd.setNotes(row.getNotes());
            onboardingBankDetailsRepository.save(obd);
        });
    }

    private void applyUpsertToEntity(
            OnboardingBankDetailsUpsertRequest req,
            OnboardingBankAccountType accountType,
            LocalDate eff,
            EmployeePayrollBank row
    ) {
        row.setAccountHolderName(req.accountHolderName().trim());
        row.setBankName(req.bankName().trim());
        row.setBranch(req.branch() != null && !req.branch().isBlank() ? req.branch().trim() : null);
        row.setAccountNumber(req.accountNumber().trim());
        row.setIfscCode(req.ifscCode().trim().toUpperCase());
        row.setAccountType(accountType);
        row.setNotes(req.notes() != null && !req.notes().isBlank() ? req.notes().trim() : null);
        row.setEffectiveFrom(eff);
    }

    private void copyFromOnboardingEntity(OnboardingBankDetails b, EmployeePayrollBank row, LocalDate eff) {
        row.setAccountHolderName(b.getAccountHolderName());
        row.setBankName(b.getBankName());
        row.setBranch(b.getBranch());
        row.setAccountNumber(b.getAccountNumber());
        row.setIfscCode(b.getIfscCode());
        row.setAccountType(b.getAccountType());
        row.setNotes(b.getNotes());
        row.setEffectiveFrom(eff);
    }

    private static OnboardingBankAccountType parseAccountType(String raw) {
        try {
            return OnboardingBankAccountType.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("accountType must be SAVINGS or CURRENT");
        }
    }

    private void recordAudit(Employee employee, String action, String detail, User actor) {
        EmployeePayrollBankAudit a = new EmployeePayrollBankAudit();
        a.setEmployee(employee);
        a.setAction(action);
        a.setDetail(detail);
        if (actor != null) {
            a.setCreatedByUserId(actor.getId());
            a.setCreatedByUsername(actor.getUsername());
        }
        auditRepository.save(a);
    }

    private String auditSnapshot(EmployeePayrollBank b) {
        return "bank=" + b.getBankName()
                + ", ifsc=" + b.getIfscCode()
                + ", acct=" + maskAccount(b.getAccountNumber())
                + ", eff=" + b.getEffectiveFrom();
    }

    private static String maskAccount(String raw) {
        if (raw == null || raw.isBlank()) {
            return "****";
        }
        String d = raw.replaceAll("\\s", "");
        if (d.length() <= 4) {
            return "****";
        }
        return "****" + d.substring(d.length() - 4);
    }

    /**
     * When a payroll row already exists, HR must choose an effective date strictly after the current active one
     * (add-new versioning; no in-place edit of the same effective date).
     */
    private void assertHrEffectiveDateAfterCurrentActive(Optional<EmployeePayrollBank> existing, LocalDate newEff) {
        if (existing.isEmpty()) {
            return;
        }
        LocalDate current = existing.get().getEffectiveFrom();
        if (!newEff.isAfter(current)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Effective date must be after the current active bank effective date (" + current
                            + "). Add a new version with a later date.");
        }
    }

    /** Rejects reuse of any effective date already present on this employee’s payroll-bank audit trail. */
    private void assertEffectiveDateNotAlreadyUsedInHistory(Long employeeId, LocalDate newEff) {
        boolean alreadyUsed = auditRepository.findByEmployee_IdOrderByCreatedAtDesc(employeeId).stream()
                .map(a -> PayrollBankAuditDto.extractEffectiveFromNewState(a.getDetail()))
                .filter(Objects::nonNull)
                .anyMatch(d -> d.equals(newEff));
        if (alreadyUsed) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "This effective date was already used for this employee. Choose a different date.");
        }
    }
}
