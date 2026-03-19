package com.hrms.payroll;

import com.hrms.auth.entity.User;
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
    private final EmployeeSalaryStructureRepository structureRepository;
    private final PayRunRepository payRunRepository;
    private final PayslipRepository payslipRepository;
    private final EmployeeRepository employeeRepository;
    private final CurrentUserService currentUserService;
    private final PayslipPdfService payslipPdfService;

    public PayrollService(
            SalaryComponentRepository salaryComponentRepository,
            EmployeeSalaryStructureRepository structureRepository,
            PayRunRepository payRunRepository,
            PayslipRepository payslipRepository,
            EmployeeRepository employeeRepository,
            CurrentUserService currentUserService,
            PayslipPdfService payslipPdfService
    ) {
        this.salaryComponentRepository = salaryComponentRepository;
        this.structureRepository = structureRepository;
        this.payRunRepository = payRunRepository;
        this.payslipRepository = payslipRepository;
        this.employeeRepository = employeeRepository;
        this.currentUserService = currentUserService;
        this.payslipPdfService = payslipPdfService;
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
    public SalaryStructureDto saveStructure(SalaryStructureRequest req) {
        requireHrAdmin();
        if (!employeeRepository.existsById(req.employeeId())) {
            throw new IllegalArgumentException("Employee not found: " + req.employeeId());
        }
        EmployeeSalaryStructure s = new EmployeeSalaryStructure();
        s.setEmployee(employeeRepository.getReferenceById(req.employeeId()));
        s.setEffectiveFrom(req.effectiveFrom());
        s.setCurrency(req.currency() != null && !req.currency().isBlank() ? req.currency().trim() : "INR");
        s.setNote(req.note());
        for (SalaryStructureLineRequest lr : req.lines()) {
            SalaryComponent comp = salaryComponentRepository.findById(lr.componentId())
                    .orElseThrow(() -> new IllegalArgumentException("Salary component not found: " + lr.componentId()));
            if (!comp.isActive()) {
                throw new IllegalArgumentException("Salary component is inactive: " + comp.getCode());
            }
            if (lr.amount().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Amount cannot be negative for component " + comp.getCode());
            }
            EmployeeSalaryStructureLine line = new EmployeeSalaryStructureLine();
            line.setStructure(s);
            line.setComponent(comp);
            line.setAmount(lr.amount());
            s.getLines().add(line);
        }
        return SalaryStructureDto.from(structureRepository.save(s));
    }

    @Transactional(readOnly = true)
    public SalaryStructureDto getLatestStructureForEmployee(Long employeeId, LocalDate asOf) {
        requireHrAdmin();
        if (!employeeRepository.existsById(employeeId)) {
            throw new IllegalArgumentException("Employee not found: " + employeeId);
        }
        LocalDate d = asOf != null ? asOf : LocalDate.now();
        List<EmployeeSalaryStructure> candidates = structureRepository.findCandidatesForPayroll(employeeId, d);
        if (candidates.isEmpty()) {
            throw new IllegalArgumentException("No salary structure for employee " + employeeId + " as of " + d);
        }
        return SalaryStructureDto.from(candidates.get(0));
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
            List<EmployeeSalaryStructure> candidates =
                    structureRepository.findCandidatesForPayroll(employee.getId(), req.periodEnd());
            if (candidates.isEmpty() || candidates.get(0).getLines().isEmpty()) {
                continue;
            }
            EmployeeSalaryStructure struct = candidates.get(0);
            BigDecimal gross = BigDecimal.ZERO;
            BigDecimal deductions = BigDecimal.ZERO;
            List<PayslipLine> slipLines = new ArrayList<>();
            for (EmployeeSalaryStructureLine sl : struct.getLines()) {
                SalaryComponent comp = sl.getComponent();
                if (!comp.isActive()) {
                    continue;
                }
                PayslipLine pl = new PayslipLine();
                pl.setComponent(comp);
                pl.setComponentCode(comp.getCode());
                pl.setComponentName(comp.getName());
                pl.setKind(comp.getKind());
                pl.setAmount(sl.getAmount());
                if (comp.getKind() == SalaryComponentKind.EARNING) {
                    gross = gross.add(sl.getAmount());
                } else {
                    deductions = deductions.add(sl.getAmount());
                }
                slipLines.add(pl);
            }
            if (slipLines.isEmpty()) {
                continue;
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
            generated++;
        }
        if (generated == 0) {
            payRunRepository.delete(run);
            throw new IllegalArgumentException("No employees with salary structure found for this period");
        }
        run.setStatus(PayRunStatus.FINALIZED);
        payRunRepository.save(run);
        return PayRunDto.from(run);
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
}
