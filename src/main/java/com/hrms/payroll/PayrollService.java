package com.hrms.payroll;

import com.hrms.advance.AdvanceStatus;
import com.hrms.advance.entity.PayslipAdvanceDeduction;
import com.hrms.advance.entity.SalaryAdvance;
import com.hrms.advance.repository.PayslipAdvanceDeductionRepository;
import com.hrms.advance.repository.SalaryAdvanceRepository;
import com.hrms.auth.entity.User;
import com.hrms.compensation.CompensationFrequency;
import com.hrms.compensation.entity.EmployeeCompensation;
import com.hrms.compensation.entity.EmployeeCompensationLine;
import com.hrms.compensation.repository.EmployeeCompensationRepository;
import com.hrms.config.PayrollStatutoryProperties;
import com.hrms.org.entity.Employee;
import com.hrms.org.repository.EmployeeRepository;
import com.hrms.payroll.dto.*;
import com.hrms.payroll.entity.*;
import com.hrms.payroll.repository.*;
import com.hrms.security.CurrentUserService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PayrollService {

    private static final String ADVANCE_RECOVERY_COMPONENT_NAME = "ADVANCE_RECOVERY";
    private static final String PF_COMPONENT_NAME = "PF";
    private static final String PT_COMPONENT_NAME = "PT";

    private final SalaryComponentRepository salaryComponentRepository;
    private final EmployeeCompensationRepository compensationRepository;
    private final PayRunRepository payRunRepository;
    private final PayslipRepository payslipRepository;
    private final EmployeeRepository employeeRepository;
    private final CurrentUserService currentUserService;
    private final PayslipPdfService payslipPdfService;
    private final SalaryAdvanceRepository salaryAdvanceRepository;
    private final PayslipAdvanceDeductionRepository payslipAdvanceDeductionRepository;
    private final PayrollStatutoryProperties payrollStatutoryProperties;
    private final PayrollFixedComponentAmountRepository payrollFixedComponentAmountRepository;

    public PayrollService(
            SalaryComponentRepository salaryComponentRepository,
            EmployeeCompensationRepository compensationRepository,
            PayRunRepository payRunRepository,
            PayslipRepository payslipRepository,
            EmployeeRepository employeeRepository,
            CurrentUserService currentUserService,
            PayslipPdfService payslipPdfService,
            SalaryAdvanceRepository salaryAdvanceRepository,
            PayslipAdvanceDeductionRepository payslipAdvanceDeductionRepository,
            PayrollStatutoryProperties payrollStatutoryProperties,
            PayrollFixedComponentAmountRepository payrollFixedComponentAmountRepository
    ) {
        this.salaryComponentRepository = salaryComponentRepository;
        this.compensationRepository = compensationRepository;
        this.payRunRepository = payRunRepository;
        this.payslipRepository = payslipRepository;
        this.employeeRepository = employeeRepository;
        this.currentUserService = currentUserService;
        this.payslipPdfService = payslipPdfService;
        this.salaryAdvanceRepository = salaryAdvanceRepository;
        this.payslipAdvanceDeductionRepository = payslipAdvanceDeductionRepository;
        this.payrollStatutoryProperties = payrollStatutoryProperties;
        this.payrollFixedComponentAmountRepository = payrollFixedComponentAmountRepository;
    }

    @Transactional(readOnly = true)
    public List<SalaryComponentDto> listComponents() {
        return salaryComponentRepository.findByActiveTrueOrderBySortOrderAsc().stream()
                .map(SalaryComponentDto::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SalaryComponentDto> listAllComponentsAdmin() {
        requireHrAdmin();
        return salaryComponentRepository.findAll().stream()
                .sorted(Comparator.comparingInt(SalaryComponent::getSortOrder).thenComparing(SalaryComponent::getName))
                .map(SalaryComponentDto::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public SalaryComponentDto createComponent(SalaryComponentRequest req) {
        requireHrAdmin();
        if (salaryComponentRepository.existsByNameIgnoreCase(req.name())) {
            throw new IllegalArgumentException("Component name already exists: " + req.name());
        }
        SalaryComponent c = new SalaryComponent();
        mapComponent(req, c);
        return SalaryComponentDto.from(salaryComponentRepository.save(c));
    }

    @Transactional
    public SalaryComponentDto updateComponent(Long id, SalaryComponentRequest req) {
        requireHrAdmin();
        SalaryComponent c = salaryComponentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Component not found: " + id));
        salaryComponentRepository.findByNameIgnoreCase(req.name()).ifPresent(other -> {
            if (!other.getId().equals(id)) {
                throw new IllegalArgumentException("Component name already exists: " + req.name());
            }
        });
        mapComponent(req, c);
        return SalaryComponentDto.from(salaryComponentRepository.save(c));
    }

    @Transactional(readOnly = true)
    public List<SalaryComponentAdminDto> listAllComponentsAdminWithFixed() {
        requireHrAdmin();

        Map<Long, PayrollFixedComponentAmount> fixedByComponentId = payrollFixedComponentAmountRepository.findAll().stream()
                .filter(a -> a.getSalaryComponent() != null && a.getSalaryComponent().getId() != null)
                .collect(Collectors.toMap(
                        a -> a.getSalaryComponent().getId(),
                        Function.identity(),
                        (a, b) -> a
                ));

        return salaryComponentRepository.findAll().stream()
                .sorted(Comparator.comparingInt(SalaryComponent::getSortOrder).thenComparing(SalaryComponent::getName))
                .map(c -> {
                    PayrollFixedComponentAmount fixed = fixedByComponentId.get(c.getId());
                    return SalaryComponentAdminDto.from(c, fixed != null ? fixed.getMonthlyAmount() : null);
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public void setFixedMonthlyAmount(Long componentId, PayrollFixedComponentUpsertRequest req) {
        requireHrAdmin();
        if (req.monthlyAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("monthlyAmount must be >= 0");
        }
        SalaryComponent comp = salaryComponentRepository.findById(componentId)
                .orElseThrow(() -> new IllegalArgumentException("Salary component not found: " + componentId));
        PayrollFixedComponentAmount row = payrollFixedComponentAmountRepository.findBySalaryComponent_Id(componentId)
                .orElseGet(() -> {
                    PayrollFixedComponentAmount n = new PayrollFixedComponentAmount();
                    n.setSalaryComponent(comp);
                    return n;
                });
        row.setSalaryComponent(comp);
        row.setMonthlyAmount(req.monthlyAmount().setScale(2, RoundingMode.HALF_UP));
        payrollFixedComponentAmountRepository.save(row);
    }

    @Transactional
    public void removeFixedComponentAmount(Long componentId) {
        requireHrAdmin();
        if (payrollFixedComponentAmountRepository.findBySalaryComponent_Id(componentId).isEmpty()) {
            throw new IllegalArgumentException("No fixed amount configured for component: " + componentId);
        }
        payrollFixedComponentAmountRepository.deleteBySalaryComponent_Id(componentId);
    }

    @Transactional
    public PayRunDto createPayRun(PayRunCreateRequest req) {
        requireHrAdmin();
        YearMonth ym = YearMonth.of(req.year(), req.month());
        LocalDate periodEnd = ym.atEndOfMonth();
        validatePayRunPeriod(req.year(), req.month());
        PayRun run = createDraftPayRun(req.year(), req.month());
        SalaryComponent advanceRecoveryComponent = findAdvanceRecoveryComponentOrNull();

        int generated = 0;
        for (Employee employee : employeeRepository.findAll()) {
            if (tryCreatePayslipForEmployee(employee, run, periodEnd, advanceRecoveryComponent)) {
                generated++;
            }
        }

        return finalizePayRunOrThrowIfNoPayslips(run, generated);
    }

    private void validatePayRunPeriod(int year, int month) {
        if (payRunRepository.existsByPayYearAndPayMonth(year, month)) {
            throw new IllegalArgumentException("A pay run already exists for this period");
        }
    }

    private PayRun createDraftPayRun(int year, int month) {
        PayRun run = new PayRun();
        run.setPayYear(year);
        run.setPayMonth(month);
        run.setStatus(PayRunStatus.DRAFT);
        return payRunRepository.save(run);
    }

    private SalaryComponent findAdvanceRecoveryComponentOrNull() {
        return salaryComponentRepository.findByNameIgnoreCase(ADVANCE_RECOVERY_COMPONENT_NAME).orElse(null);
    }

    private PayRunDto finalizePayRunOrThrowIfNoPayslips(PayRun run, int payslipCount) {
        if (payslipCount == 0) {
            payRunRepository.delete(run);
            throw new IllegalArgumentException("No employees with compensation found for this period");
        }
        run.setStatus(PayRunStatus.FINALIZED);
        payRunRepository.save(run);
        return PayRunDto.from(run);
    }

    private boolean tryCreatePayslipForEmployee(
            Employee employee,
            PayRun run,
            LocalDate periodEnd,
            SalaryComponent advanceRecoveryComponent
    ) {
        Optional<EmployeeCompensation> compensation = findActiveCompensation(employee.getId(), periodEnd);
        if (compensation.isEmpty() || compensation.get().getLines().isEmpty()) {
            return false;
        }

        PayslipDraft draft = new PayslipDraft();
        addCompensationLines(compensation.get(), draft);
        if (draft.slipLines.isEmpty()) {
            return false;
        }

        List<AdvanceRecovery> recoveries =
                addAdvanceRecoveryLines(employee, advanceRecoveryComponent, draft);

        Payslip payslip = persistPayslip(run, employee, draft);
        applyAdvanceRecoveries(payslip, recoveries);
        return true;
    }

    private Optional<EmployeeCompensation> findActiveCompensation(Long employeeId, LocalDate periodEnd) {
        List<EmployeeCompensation> candidates = compensationRepository.findActiveAsOf(employeeId, periodEnd);
        if (candidates.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(candidates.get(0));
    }

    private void addCompensationLines(EmployeeCompensation compensation, PayslipDraft draft) {
        for (EmployeeCompensationLine cl : compensation.getLines()) {
            SalaryComponent comp = cl.getComponent();
            if (!comp.isActive()) {
                continue;
            }
            BigDecimal monthlyAmount = convertToMonthlyAmount(cl);
            if (monthlyAmount.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            PayslipLine pl = new PayslipLine();
            pl.setComponent(comp);
            pl.setComponentCode(deriveComponentCode(comp));
            pl.setComponentName(comp.getName());
            pl.setKind(comp.getKind());
            pl.setAmount(monthlyAmount);
            if (comp.getKind() == SalaryComponentKind.EARNING) {
                draft.gross = draft.gross.add(monthlyAmount);
            } else {
                draft.deductions = draft.deductions.add(monthlyAmount);
            }
            draft.slipLines.add(pl);
        }
    }

    private void addStatutoryDeductionLinesLegacy(PayslipDraft draft) {
        addConfiguredDeductionIfAbsent(draft, PF_COMPONENT_NAME,
                payrollStatutoryProperties.getProvidentFundMonthly());
        addConfiguredDeductionIfAbsent(draft, PT_COMPONENT_NAME,
                payrollStatutoryProperties.getProfessionalTaxMonthly());
    }

    private void addFixedOrgLine(PayslipDraft draft, SalaryComponent comp, BigDecimal amount) {
        PayslipLine pl = new PayslipLine();
        pl.setComponent(comp);
        pl.setComponentCode(deriveComponentCode(comp));
        pl.setComponentName(comp.getName());
        pl.setKind(comp.getKind());
        pl.setAmount(amount);
        draft.slipLines.add(pl);
        if (comp.getKind() == SalaryComponentKind.EARNING) {
            draft.gross = draft.gross.add(amount);
        } else {
            draft.deductions = draft.deductions.add(amount);
        }
    }

    private void addConfiguredDeductionIfAbsent(PayslipDraft draft, String componentName, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        if (draftHasComponentCode(draft, componentName)) {
            return;
        }
        salaryComponentRepository.findByNameIgnoreCase(componentName).ifPresent(comp -> {
            if (!comp.isActive() || comp.getKind() != SalaryComponentKind.DEDUCTION) {
                return;
            }
            PayslipLine pl = new PayslipLine();
            pl.setComponent(comp);
            pl.setComponentCode(deriveComponentCode(comp));
            pl.setComponentName(comp.getName());
            pl.setKind(SalaryComponentKind.DEDUCTION);
            pl.setAmount(amount);
            draft.slipLines.add(pl);
            draft.deductions = draft.deductions.add(amount);
        });
    }

    private static boolean draftHasComponentCode(PayslipDraft draft, String code) {
        for (PayslipLine pl : draft.slipLines) {
            if (pl.getComponentCode() != null && pl.getComponentCode().equalsIgnoreCase(code)) {
                return true;
            }
        }
        return false;
    }

    private List<AdvanceRecovery> addAdvanceRecoveryLines(
            Employee employee,
            SalaryComponent advanceComp,
            PayslipDraft draft
    ) {
        List<AdvanceRecovery> recoveries = new ArrayList<>();
        if (advanceComp == null) {
            return recoveries;
        }
        List<SalaryAdvance> recoverable = salaryAdvanceRepository
                .findByEmployeeIdAndStatusAndOutstandingBalanceGreaterThan(
                        employee.getId(), AdvanceStatus.PAID, BigDecimal.ZERO);
        for (SalaryAdvance adv : recoverable) {
            if (adv.getOutstandingBalance() == null
                    || adv.getOutstandingBalance().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal per = adv.getRecoveryAmountPerMonth() != null
                    ? adv.getRecoveryAmountPerMonth()
                    : adv.getOutstandingBalance();
            BigDecimal take = per.min(adv.getOutstandingBalance());
            if (take.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            PayslipLine pl = new PayslipLine();
            pl.setComponent(advanceComp);
            pl.setComponentCode(deriveComponentCode(advanceComp));
            pl.setComponentName(advanceComp.getName() + " (#" + adv.getId() + ")");
            pl.setKind(SalaryComponentKind.DEDUCTION);
            pl.setAmount(take);
            draft.slipLines.add(pl);
            draft.deductions = draft.deductions.add(take);
            recoveries.add(new AdvanceRecovery(adv, take));
        }
        return recoveries;
    }

    private Payslip persistPayslip(PayRun run, Employee employee, PayslipDraft draft) {
        BigDecimal net = draft.gross.subtract(draft.deductions);
        Payslip payslip = new Payslip();
        payslip.setPayRun(run);
        payslip.setEmployee(employee);
        payslip.setGrossAmount(draft.gross);
        payslip.setDeductionAmount(draft.deductions);
        payslip.setNetAmount(net);
        for (PayslipLine pl : draft.slipLines) {
            pl.setPayslip(payslip);
            payslip.getLines().add(pl);
        }
        return payslipRepository.save(payslip);
    }

    private void applyAdvanceRecoveries(Payslip payslip, List<AdvanceRecovery> recoveries) {
        for (AdvanceRecovery ar : recoveries) {
            SalaryAdvance adv = ar.advance();
            BigDecimal newBal = adv.getOutstandingBalance().subtract(ar.amount());
            if (newBal.compareTo(BigDecimal.ZERO) < 0) {
                newBal = BigDecimal.ZERO;
            }
            adv.setOutstandingBalance(newBal);
            if (newBal.compareTo(BigDecimal.ZERO) <= 0) {
                adv.setStatus(AdvanceStatus.RECOVERY_COMPLETE);
            }
            salaryAdvanceRepository.save(adv);
            PayslipAdvanceDeduction pad = new PayslipAdvanceDeduction();
            pad.setPayslip(payslip);
            pad.setAdvance(adv);
            pad.setAmount(ar.amount());
            payslipAdvanceDeductionRepository.save(pad);
        }
    }

    private static final class PayslipDraft {
        BigDecimal gross = BigDecimal.ZERO;
        BigDecimal deductions = BigDecimal.ZERO;
        final List<PayslipLine> slipLines = new ArrayList<>();
    }

    private BigDecimal convertToMonthlyAmount(EmployeeCompensationLine line) {
        BigDecimal amount = line.getAmount() != null ? line.getAmount() : BigDecimal.ZERO;
        CompensationFrequency freq = line.getFrequency() != null ? line.getFrequency() : CompensationFrequency.MONTHLY;

        return switch (freq) {
            case MONTHLY -> amount;
            case YEARLY -> amount.divide(BigDecimal.valueOf(12), 2, java.math.RoundingMode.HALF_UP); //TODO::add in particular month salary.
            case ONE_TIME -> BigDecimal.ZERO; // One-time payments not included in regular payroll
        };
    }

    @Transactional(readOnly = true)
    public List<PayRunDto> listPayRuns() {
        requireHrAdmin();
        return payRunRepository.findAll().stream()
                .sorted(Comparator.comparing(PayRun::getId).reversed())
                .map(PayRunDto::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PayslipDto> listPayslipsForRun(Long payRunId) {
        requireHrAdmin();
        if (!payRunRepository.existsById(payRunId)) {
            throw new IllegalArgumentException("Pay run not found: " + payRunId);
        }
        return payslipRepository.findByPayRunIdOrderByEmployeeId(payRunId).stream()
                .sorted(Comparator.comparing(p -> p.getEmployee().getId()))
                .map(this::toPayslipDtoSorted)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PayslipDto> listMyPayslips(Long payRunId) {
        User u = currentUserService.requireCurrentUser();
        if (u.getEmployee() == null) {
            throw new IllegalArgumentException("No employee linked to your user");
        }
        Long empId = u.getEmployee().getId();
        List<Payslip> list = payslipRepository.findByEmployeeIdOrderByIdDesc(empId);
        if (payRunId != null) {
            list = list.stream().filter(p -> p.getPayRun().getId().equals(payRunId)).collect(Collectors.toList());
        }
        return list.stream().map(this::toPayslipDtoSorted).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PayslipDto getPayslip(Long id) {
        Payslip p = payslipRepository.findByIdWithLines(id)
                .orElseThrow(() -> new IllegalArgumentException("Payslip not found: " + id));
        assertCanViewPayslip(p);
        return toPayslipDtoSorted(p);
    }

    @Transactional
    public byte[] payslipPdf(Long id) {
        Payslip p = payslipRepository.findByIdWithLines(id)
                .orElseThrow(() -> new IllegalArgumentException("Payslip not found: " + id));
        assertCanViewPayslip(p);
        try {
            byte[] bytes = payslipPdfService.build(p, p.getPayRun());
            p.setPdfGeneratedAt(Instant.now());
            payslipRepository.save(p);
            return bytes;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to generate PDF: " + e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private PayslipDto toPayslipDtoSorted(Payslip p) {
        p.getLines().sort(Comparator
                .comparing(PayslipLine::getKind)
                .thenComparing(PayslipLine::getComponentCode));
        return PayslipDto.from(p);
    }

    private void assertCanViewPayslip(Payslip p) {
        User u = currentUserService.requireCurrentUser();
        if (isHrOrAdmin(u)) {
            return;
        }
        if (u.getEmployee() != null && Objects.equals(u.getEmployee().getId(), p.getEmployee().getId())) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot view this payslip");
    }

    private void requireHrAdmin() {
        User u = currentUserService.requireCurrentUser();
        if (!isHrOrAdmin(u)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "HR or Admin only");
        }
    }

    private static boolean isHrOrAdmin(User u) {
        return u.getRoles().stream().anyMatch(r ->
                "ROLE_HR".equals(r.getName()) || "ROLE_ADMIN".equals(r.getName()));
    }

    private static void mapComponent(SalaryComponentRequest req, SalaryComponent c) {
        c.setName(req.name().trim());
        c.setKind(req.kind());
        c.setSortOrder(req.sortOrder());
        c.setActive(req.active());
    }

    private static String deriveComponentCode(SalaryComponent c) {
        String n = c.getName() != null ? c.getName() : "";
        String trimmed = n.trim();
        if (trimmed.isEmpty()) {
            return c.getId() != null ? "COMP_" + c.getId() : "COMP";
        }
        return trimmed
                .toUpperCase()
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
    }

    private record AdvanceRecovery(SalaryAdvance advance, BigDecimal amount) {}
}
