package com.hrms.advance;

import com.hrms.auth.entity.User;
import com.hrms.advance.dto.*;
import com.hrms.advance.entity.SalaryAdvance;
import com.hrms.advance.repository.SalaryAdvanceRepository;
import com.hrms.org.entity.Employee;
import com.hrms.org.repository.EmployeeRepository;
import com.hrms.security.CurrentUserService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class SalaryAdvanceService {

    private final SalaryAdvanceRepository advanceRepository;
    private final EmployeeRepository employeeRepository;
    private final CurrentUserService currentUserService;

    public SalaryAdvanceService(
            SalaryAdvanceRepository advanceRepository,
            EmployeeRepository employeeRepository,
            CurrentUserService currentUserService
    ) {
        this.advanceRepository = advanceRepository;
        this.employeeRepository = employeeRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public List<SalaryAdvanceDto> listAll() {
        requireHrAdmin();
        return advanceRepository.findAllByOrderByIdDesc().stream()
                .map(SalaryAdvanceDto::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SalaryAdvanceDto> listMine() {
        User u = currentUserService.requireCurrentUser();
        if (u.getEmployee() == null) {
            throw new IllegalArgumentException("No employee linked to your user");
        }
        return advanceRepository.findByEmployeeIdOrderByIdDesc(u.getEmployee().getId()).stream()
                .map(SalaryAdvanceDto::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public SalaryAdvanceDto create(SalaryAdvanceCreateRequest req) {
        User u = currentUserService.requireCurrentUser();
        Long targetEmpId = req.employeeId();
        if (targetEmpId == null) {
            if (u.getEmployee() == null) {
                throw new IllegalArgumentException("Specify employeeId or link your user to an employee");
            }
            targetEmpId = u.getEmployee().getId();
        } else if (!isHrOrAdmin(u)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only HR/Admin can create advances for others");
        }
        if (!employeeRepository.existsById(targetEmpId)) {
            throw new IllegalArgumentException("Employee not found: " + targetEmpId);
        }
        long open = advanceRepository.findByEmployeeIdOrderByIdDesc(targetEmpId).stream()
                .filter(a -> a.getStatus() == AdvanceStatus.PENDING
                        || a.getStatus() == AdvanceStatus.APPROVED
                        || a.getStatus() == AdvanceStatus.PAID)
                .count();
        if (open >= 2) {
            throw new IllegalArgumentException("Too many open advances; resolve existing ones first");
        }

        SalaryAdvance a = new SalaryAdvance();
        a.setEmployee(employeeRepository.getReferenceById(targetEmpId));
        a.setAmount(req.amount());
        a.setReason(req.reason());
        a.setRecoveryMonths(Math.max(1, req.recoveryMonths()));
        a.setStatus(AdvanceStatus.PENDING);
        return SalaryAdvanceDto.from(advanceRepository.save(a));
    }

    @Transactional
    public SalaryAdvanceDto approve(Long id, SalaryAdvanceApproveRequest req) {
        User u = currentUserService.requireCurrentUser();
        SalaryAdvance a = advanceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Advance not found: " + id));
        Employee subject = employeeRepository.findById(a.getEmployee().getId())
                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));
        if (!canApproveAdvance(u, subject)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to approve");
        }
        if (a.getStatus() != AdvanceStatus.PENDING) {
            throw new IllegalArgumentException("Advance is not pending");
        }
        int months = req.recoveryMonths() > 0 ? req.recoveryMonths() : a.getRecoveryMonths();
        a.setRecoveryMonths(months);
        BigDecimal per = req.recoveryAmountPerMonth();
        if (per == null || per.compareTo(BigDecimal.ZERO) <= 0) {
            per = a.getAmount().divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP);
        }
        a.setRecoveryAmountPerMonth(per);
        a.setStatus(AdvanceStatus.APPROVED);
        a.setApprovedBy(u);
        a.setApprovedAt(Instant.now());
        return SalaryAdvanceDto.from(advanceRepository.save(a));
    }

    @Transactional
    public SalaryAdvanceDto reject(Long id, SalaryAdvanceRejectRequest req) {
        User u = currentUserService.requireCurrentUser();
        SalaryAdvance a = advanceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Advance not found: " + id));
        Employee subject = employeeRepository.findById(a.getEmployee().getId()).orElseThrow();
        if (!canApproveAdvance(u, subject)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to reject");
        }
        if (a.getStatus() != AdvanceStatus.PENDING) {
            throw new IllegalArgumentException("Advance is not pending");
        }
        a.setStatus(AdvanceStatus.REJECTED);
        a.setRejectedReason(req.reason());
        return SalaryAdvanceDto.from(advanceRepository.save(a));
    }

    @Transactional
    public SalaryAdvanceDto markPaid(Long id, LocalDate payoutDate) {
        requireHrAdmin();
        SalaryAdvance a = advanceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Advance not found: " + id));
        if (a.getStatus() != AdvanceStatus.APPROVED) {
            throw new IllegalArgumentException("Advance must be approved before marking paid");
        }
        a.setStatus(AdvanceStatus.PAID);
        a.setPayoutDate(payoutDate != null ? payoutDate : LocalDate.now());
        a.setPaidAt(Instant.now());
        a.setOutstandingBalance(a.getAmount());
        return SalaryAdvanceDto.from(advanceRepository.save(a));
    }

    private boolean canApproveAdvance(User u, Employee subject) {
        if (isHrOrAdmin(u)) {
            return true;
        }
        if (u.getEmployee() == null) {
            return false;
        }
        return subject.getManager() != null
                && Objects.equals(subject.getManager().getId(), u.getEmployee().getId());
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
