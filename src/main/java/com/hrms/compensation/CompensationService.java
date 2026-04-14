package com.hrms.compensation;

import com.hrms.auth.entity.User;
import com.hrms.compensation.dto.*;
import com.hrms.compensation.entity.EmployeeCompensation;
import com.hrms.compensation.entity.EmployeeCompensationLine;
import com.hrms.compensation.repository.EmployeeCompensationRepository;
import com.hrms.org.repository.EmployeeRepository;
import com.hrms.payroll.PayrollService;
import com.hrms.payroll.entity.SalaryComponent;
import com.hrms.payroll.repository.SalaryComponentRepository;
import com.hrms.security.CurrentUserService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CompensationService {

    private final EmployeeCompensationRepository compensationRepository;
    private final EmployeeRepository employeeRepository;
    private final SalaryComponentRepository salaryComponentRepository;
    private final CurrentUserService currentUserService;
    private final PayrollService payrollService;

    public CompensationService(
            EmployeeCompensationRepository compensationRepository,
            EmployeeRepository employeeRepository,
            SalaryComponentRepository salaryComponentRepository,
            CurrentUserService currentUserService,
            PayrollService payrollService
    ) {
        this.compensationRepository = compensationRepository;
        this.employeeRepository = employeeRepository;
        this.salaryComponentRepository = salaryComponentRepository;
        this.currentUserService = currentUserService;
        this.payrollService = payrollService;
    }

    @Transactional(readOnly = true)
    public List<CompensationDto> listForEmployee(Long employeeId) {
        requireHrAdmin();
        if (!employeeRepository.existsById(employeeId)) {
            throw new IllegalArgumentException("Employee not found: " + employeeId);
        }
        return compensationRepository.findByEmployeeIdOrderByEffectiveFromDesc(employeeId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Optional filters: both null returns all compensations (newest first).
     * Employee only: all packages for that employee.
     * Effective date only: all packages active on that date (any employee).
     * Both: packages active for that employee on that date.
     */
    @Transactional(readOnly = true)
    public List<CompensationDto> search(Long employeeId, java.time.LocalDate effectiveDate) {
        requireHrAdmin();
        if (employeeId != null && effectiveDate != null) {
            if (!employeeRepository.existsById(employeeId)) {
                throw new IllegalArgumentException("Employee not found: " + employeeId);
            }
            return compensationRepository.findActiveAsOf(employeeId, effectiveDate).stream()
                    .map(this::toDto)
                    .toList();
        }
        if (employeeId != null) {
            if (!employeeRepository.existsById(employeeId)) {
                throw new IllegalArgumentException("Employee not found: " + employeeId);
            }
            return compensationRepository.findByEmployeeIdOrderByEffectiveFromDesc(employeeId).stream()
                    .map(this::toDto)
                    .toList();
        }
        if (effectiveDate != null) {
            return compensationRepository.findAllActiveAsOf(effectiveDate).stream()
                    .map(this::toDto)
                    .toList();
        }
        return compensationRepository.findAllOrderByEffectiveFromDesc().stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public CompensationDto create(CompensationCreateRequest req) {
        requireHrAdmin();
        if (!employeeRepository.existsById(req.employeeId())) {
            throw new IllegalArgumentException("Employee not found: " + req.employeeId());
        }
        EmployeeCompensation c = new EmployeeCompensation();
        c.setEmployee(employeeRepository.getReferenceById(req.employeeId()));
        c.setEffectiveFrom(req.effectiveFrom());
        c.setEffectiveTo(req.effectiveTo());
        c.setAnnualCtc(req.annualCtc());
        c.setNotes(req.notes());
        for (CompensationLineRequest lr : req.lines()) {
            SalaryComponent comp = salaryComponentRepository.findById(lr.componentId())
                    .orElseThrow(() -> new IllegalArgumentException("Salary component not found: " + lr.componentId()));
            EmployeeCompensationLine line = new EmployeeCompensationLine();
            line.setCompensation(c);
            line.setComponent(comp);
            line.setAmount(lr.amount());
            line.setFrequency(lr.frequency());
            c.getLines().add(line);
        }
        return toDto(compensationRepository.save(c));
    }


    private CompensationDto toDto(EmployeeCompensation c) {
        return CompensationDto.from(c);
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
}
