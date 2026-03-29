package com.hrms.leave;

import com.hrms.auth.entity.User;
import com.hrms.leave.dto.LeaveBalanceAdjustRequest;
import com.hrms.leave.dto.LeaveBalanceAdjustmentDto;
import com.hrms.leave.dto.LeaveBalanceDto;
import com.hrms.leave.dto.LeaveBalanceUpsertRequest;
import com.hrms.leave.entity.LeaveBalance;
import com.hrms.leave.entity.LeaveBalanceAdjustment;
import com.hrms.leave.entity.LeaveType;
import com.hrms.leave.repository.LeaveBalanceAdjustmentRepository;
import com.hrms.leave.repository.LeaveBalanceRepository;
import com.hrms.leave.repository.LeaveTypeRepository;
import com.hrms.org.repository.EmployeeRepository;
import com.hrms.security.CurrentUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LeaveBalanceService {

    private final LeaveBalanceRepository leaveBalanceRepository;
    private final LeaveBalanceAdjustmentRepository adjustmentRepository;
    private final EmployeeRepository employeeRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final CurrentUserService currentUserService;

    public LeaveBalanceService(
            LeaveBalanceRepository leaveBalanceRepository,
            LeaveBalanceAdjustmentRepository adjustmentRepository,
            EmployeeRepository employeeRepository,
            LeaveTypeRepository leaveTypeRepository,
            CurrentUserService currentUserService
    ) {
        this.leaveBalanceRepository = leaveBalanceRepository;
        this.adjustmentRepository = adjustmentRepository;
        this.employeeRepository = employeeRepository;
        this.leaveTypeRepository = leaveTypeRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public List<LeaveBalanceDto> list(Long employeeId, int year) {
        assertCanViewBalances(employeeId);
        if (!employeeRepository.existsById(employeeId)) {
            throw new IllegalArgumentException("Employee not found: " + employeeId);
        }
        return leaveBalanceRepository.findByEmployeeIdAndYear(employeeId, year).stream()
                .map(LeaveBalanceDto::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<LeaveBalanceAdjustmentDto> listAdjustments(Long employeeId, int year) {
        assertCanViewBalances(employeeId);
        if (!employeeRepository.existsById(employeeId)) {
            throw new IllegalArgumentException("Employee not found: " + employeeId);
        }
        return adjustmentRepository.findHistoryForEmployeeYear(employeeId, year).stream()
                .map(LeaveBalanceAdjustmentDto::from)
                .collect(Collectors.toList());
    }

    private void assertCanViewBalances(Long employeeId) {
        User u = currentUserService.requireCurrentUser();
        if (!isHrOrAdmin(u)) {
            if (u.getEmployee() == null || !u.getEmployee().getId().equals(employeeId)) {
                throw new IllegalArgumentException("You can only view your own leave balances");
            }
        }
    }

    @Transactional
    public LeaveBalanceDto adjust(LeaveBalanceAdjustRequest req) {
        User u = currentUserService.requireCurrentUser();
        if (!isHrOrAdmin(u)) {
            throw new IllegalArgumentException("Only HR/Admin can manage leave balances");
        }
        if (!employeeRepository.existsById(req.employeeId())) {
            throw new IllegalArgumentException("Employee not found: " + req.employeeId());
        }
        LeaveType leaveType = leaveTypeRepository.findById(req.leaveTypeId())
                .orElseThrow(() -> new IllegalArgumentException("Leave type not found: " + req.leaveTypeId()));
        if (req.deltaDays().compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("deltaDays must be non-zero");
        }
        if (req.kind() == LeaveBalanceAdjustmentKind.CARRY_FORWARD && !leaveType.isCarryForward()) {
            throw new IllegalArgumentException("This leave type does not allow carry-forward");
        }

        LeaveBalance b = leaveBalanceRepository
                .findByEmployeeIdAndLeaveTypeIdAndYear(req.employeeId(), req.leaveTypeId(), req.year())
                .orElseGet(() -> newEmptyBalance(req.employeeId(), leaveType, req.year()));

        BigDecimal nextAlloc = b.getAllocatedDays();
        BigDecimal nextCarry = b.getCarryForwardedDays();
        if (req.kind() == LeaveBalanceAdjustmentKind.ALLOCATION) {
            nextAlloc = nextAlloc.add(req.deltaDays());
        } else if (req.kind() == LeaveBalanceAdjustmentKind.CARRY_FORWARD) {
            nextCarry = nextCarry.add(req.deltaDays());
        } else if (req.kind() == LeaveBalanceAdjustmentKind.LAPSE) {
            if (req.deltaDays().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Lapse requires a positive number of days");
            }
            BigDecimal remaining = req.deltaDays();
            BigDecimal fromAlloc = nextAlloc.min(remaining);
            nextAlloc = nextAlloc.subtract(fromAlloc);
            remaining = remaining.subtract(fromAlloc);
            if (remaining.compareTo(BigDecimal.ZERO) > 0) {
                if (nextCarry.compareTo(remaining) < 0) {
                    throw new IllegalArgumentException("Cannot lapse more than allocation + carry-forward pool");
                }
                nextCarry = nextCarry.subtract(remaining);
            }
        }

        if (nextAlloc.compareTo(BigDecimal.ZERO) < 0 || nextCarry.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Balance cannot go negative");
        }

        BigDecimal pool = nextAlloc.add(nextCarry);
        if (pool.compareTo(b.getUsedDays()) < 0) {
            throw new IllegalArgumentException(
                    "Total pool (allocated + carry-forward) cannot be less than used days (" + b.getUsedDays() + ")");
        }

        if (req.kind() == LeaveBalanceAdjustmentKind.CARRY_FORWARD) {
            if (leaveType.getMaxCarryForward() != null && nextCarry.compareTo(leaveType.getMaxCarryForward()) > 0) {
                throw new IllegalArgumentException(
                        "Carry-forward cannot exceed leave type max (" + leaveType.getMaxCarryForward() + " days)");
            }
            if (leaveType.getMaxCarryForwardPerYear() != null
                    && req.deltaDays().compareTo(BigDecimal.ZERO) > 0
                    && req.deltaDays().compareTo(leaveType.getMaxCarryForwardPerYear()) > 0) {
                throw new IllegalArgumentException(
                        "This adjustment exceeds max carry-forward per year (" + leaveType.getMaxCarryForwardPerYear() + " days)");
            }
        }

        b.setAllocatedDays(nextAlloc);
        b.setCarryForwardedDays(nextCarry);
        LeaveBalance saved = leaveBalanceRepository.save(b);

        LeaveBalanceAdjustment adj = new LeaveBalanceAdjustment();
        adj.setLeaveBalance(saved);
        adj.setKind(req.kind());
        adj.setDeltaDays(req.deltaDays());
        adj.setCommentText(req.comment().trim());
        adj.setCreatedBy(u);
        adjustmentRepository.save(adj);

        return LeaveBalanceDto.from(saved);
    }

    private LeaveBalance newEmptyBalance(Long employeeId, LeaveType leaveType, int year) {
        LeaveBalance nb = new LeaveBalance();
        nb.setEmployee(employeeRepository.getReferenceById(employeeId));
        nb.setLeaveType(leaveType);
        nb.setYear(year);
        nb.setUsedDays(BigDecimal.ZERO);
        nb.setAllocatedDays(BigDecimal.ZERO);
        nb.setCarryForwardedDays(BigDecimal.ZERO);
        return nb;
    }

    @Transactional
    public LeaveBalanceDto upsert(LeaveBalanceUpsertRequest req) {
        User u = currentUserService.requireCurrentUser();
        if (!isHrOrAdmin(u)) {
            throw new IllegalArgumentException("Only HR/Admin can manage leave balances");
        }
        if (!employeeRepository.existsById(req.employeeId())) {
            throw new IllegalArgumentException("Employee not found: " + req.employeeId());
        }
        if (!leaveTypeRepository.existsById(req.leaveTypeId())) {
            throw new IllegalArgumentException("Leave type not found: " + req.leaveTypeId());
        }
        LeaveBalance b = leaveBalanceRepository
                .findByEmployeeIdAndLeaveTypeIdAndYear(req.employeeId(), req.leaveTypeId(), req.year())
                .orElseGet(LeaveBalance::new);
        if (b.getId() == null) {
            b.setEmployee(employeeRepository.getReferenceById(req.employeeId()));
            b.setLeaveType(leaveTypeRepository.getReferenceById(req.leaveTypeId()));
            b.setYear(req.year());
            b.setUsedDays(BigDecimal.ZERO);
            b.setCarryForwardedDays(BigDecimal.ZERO);
        }
        BigDecimal pool = req.allocatedDays().add(b.getCarryForwardedDays());
        if (pool.compareTo(b.getUsedDays()) < 0) {
            throw new IllegalArgumentException(
                    "Allocated + carry-forward cannot be less than used days (" + b.getUsedDays() + ")");
        }
        b.setAllocatedDays(req.allocatedDays());
        return LeaveBalanceDto.from(leaveBalanceRepository.save(b));
    }

    private static boolean isHrOrAdmin(User u) {
        return u.getRoles().stream().anyMatch(r ->
                "ROLE_HR".equals(r.getName()) || "ROLE_ADMIN".equals(r.getName()));
    }
}
