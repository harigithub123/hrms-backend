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
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class PayrollService {

    private final SalaryComponentRepository salaryComponentRepository;
    private final EmployeeCompensationRepository compensationRepository;
    private final PayRunRepository payRunRepository;
    private final PayslipRepository payslipRepository;
    private final EmployeeRepository employeeRepository;
    private final CurrentUserService currentUserService;
    private final PayslipPdfService payslipPdfService;
    private final SalaryAdvanceRepository salaryAdvanceRepository;
    private final PayslipAdvanceDeductionRepository payslipAdvanceDeductionRepository;

    public PayrollService(
            SalaryComponentRepository salaryComponentRepository,
            EmployeeCompensationRepository compensationRepository,
            PayRunRepository payRunRepository,
            PayslipRepository payslipRepository,
            EmployeeRepository employeeRepository,
            CurrentUserService currentUserService,
            PayslipPdfService payslipPdfService,
            SalaryAdvanceRepository salaryAdvanceRepository,
            PayslipAdvanceDeductionRepository payslipAdvanceDeductionRepository
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
                .sorted(Comparator.comparingInt(SalaryComponent::getSortOrder).thenComparing(SalaryComponent::getCode))
                .map(SalaryComponentDto::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public SalaryComponentDto createComponent(SalaryComponentRequest req) {
        requireHrAdmin();
        if (salaryComponentRepository.existsByCodeIgnoreCase(req.code())) {
            throw new IllegalArgumentException("Component code already exists: " + req.code());
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
        salaryComponentRepository.findByCodeIgnoreCase(req.code()).ifPresent(other -> {
            if (!other.getId().equals(id)) {
                throw new IllegalArgumentException("Component code already exists: " + req.code());
            }
        });
        mapComponent(req, c);
        return SalaryComponentDto.from(salaryComponentRepository.save(c));
    }


    @Transactional
    public PayRunDto createPayRun(PayRunCreateRequest req) {
        requireHrAdmin();
        if (req.periodEnd().isBefore(req.periodStart())) {
            throw new IllegalArgumentException("periodEnd must be on or after periodStart");
        }
        if (payRunRepository.existsByPeriodStartAndPeriodEnd(req.periodStart(), req.periodEnd())) {
            throw new IllegalArgumentException("A pay run already exists for this period");
        }
        PayRun run = new PayRun();
        run.setPeriodStart(req.periodStart());
        run.setPeriodEnd(req.periodEnd());
        run.setStatus(PayRunStatus.DRAFT);
        payRunRepository.save(run);

        int generated = 0;
        for (var employee : employeeRepository.findAll()) {
            List<EmployeeCompensation> candidates =
                    compensationRepository.findActiveAsOf(employee.getId(), req.periodEnd());
            if (candidates.isEmpty()) {
                continue;
            }
            EmployeeCompensation compensation = candidates.get(0);
            if (compensation.getLines().isEmpty()) {
                continue;
            }

            BigDecimal gross = BigDecimal.ZERO;
            BigDecimal deductions = BigDecimal.ZERO;
            List<PayslipLine> slipLines = new ArrayList<>();

            for (EmployeeCompensationLine cl : compensation.getLines()) {
                SalaryComponent comp = cl.getComponent();
                if (!comp.isActive()) {
                    continue;
                }

                // Convert to monthly amount for payslip
                BigDecimal monthlyAmount = convertToMonthlyAmount(cl);
                if (monthlyAmount.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }

                PayslipLine pl = new PayslipLine();
                pl.setComponent(comp);
                pl.setComponentCode(comp.getCode());
                pl.setComponentName(comp.getName());
                pl.setKind(comp.getKind());
                pl.setAmount(monthlyAmount);

                if (comp.getKind() == SalaryComponentKind.EARNING) {
                    gross = gross.add(monthlyAmount);
                } else {
                    deductions = deductions.add(monthlyAmount);
                }
                slipLines.add(pl);
            }

            if (slipLines.isEmpty()) {
                continue;
            }

            SalaryComponent advanceComp = salaryComponentRepository.findByCodeIgnoreCase("ADVANCE_RECOVERY").orElse(null);
            List<AdvanceRecovery> advanceRecoveries = new ArrayList<>();
            if (advanceComp != null) {
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
                    pl.setComponentCode(advanceComp.getCode());
                    pl.setComponentName(advanceComp.getName() + " (#" + adv.getId() + ")");
                    pl.setKind(SalaryComponentKind.DEDUCTION);
                    pl.setAmount(take);
                    slipLines.add(pl);
                    deductions = deductions.add(take);
                    advanceRecoveries.add(new AdvanceRecovery(adv.getId(), take));
                }
            }

            BigDecimal net = gross.subtract(deductions);
            Payslip payslip = new Payslip();
            payslip.setPayRun(run);
            payslip.setEmployee(employee);
            payslip.setGrossAmount(gross);
            payslip.setDeductionAmount(deductions);
            payslip.setNetAmount(net);
            for (PayslipLine pl : slipLines) {
                pl.setPayslip(payslip);
                payslip.getLines().add(pl);
            }
            payslipRepository.save(payslip);

            for (AdvanceRecovery ar : advanceRecoveries) {
                SalaryAdvance adv = salaryAdvanceRepository.findById(ar.advanceId())
                        .orElseThrow(() -> new IllegalStateException("Advance missing: " + ar.advanceId()));
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
            generated++;
        }

        if (generated == 0) {
            payRunRepository.delete(run);
            throw new IllegalArgumentException("No employees with compensation found for this period");
        }
        run.setStatus(PayRunStatus.FINALIZED);
        payRunRepository.save(run);
        return PayRunDto.from(run);
    }

    private BigDecimal convertToMonthlyAmount(EmployeeCompensationLine line) {
        BigDecimal amount = line.getAmount() != null ? line.getAmount() : BigDecimal.ZERO;
        CompensationFrequency freq = line.getFrequency() != null ? line.getFrequency() : CompensationFrequency.MONTHLY;

        return switch (freq) {
            case MONTHLY -> amount;
            case YEARLY -> amount.divide(BigDecimal.valueOf(12), 2, java.math.RoundingMode.HALF_UP);
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
        c.setCode(req.code().trim().toUpperCase());
        c.setName(req.name().trim());
        c.setKind(req.kind());
        c.setSortOrder(req.sortOrder());
        c.setActive(req.active());
    }

    private record AdvanceRecovery(Long advanceId, BigDecimal amount) {}
}
