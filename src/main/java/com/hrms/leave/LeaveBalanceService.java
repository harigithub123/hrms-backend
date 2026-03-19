package com.hrms.leave;

import com.hrms.auth.entity.User;
import com.hrms.leave.dto.LeaveBalanceDto;
import com.hrms.leave.dto.LeaveBalanceUpsertRequest;
import com.hrms.leave.entity.LeaveBalance;
import com.hrms.leave.repository.LeaveBalanceRepository;
import com.hrms.leave.repository.LeaveTypeRepository;
import com.hrms.org.repository.EmployeeRepository;
import com.hrms.security.CurrentUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class LeaveBalanceService {

    private final LeaveBalanceRepository leaveBalanceRepository;
    private final EmployeeRepository employeeRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final CurrentUserService currentUserService;

    public LeaveBalanceService(
            LeaveBalanceRepository leaveBalanceRepository,
            EmployeeRepository employeeRepository,
            LeaveTypeRepository leaveTypeRepository,
            CurrentUserService currentUserService
    ) {
        this.leaveBalanceRepository = leaveBalanceRepository;
        this.employeeRepository = employeeRepository;
        this.leaveTypeRepository = leaveTypeRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public List<LeaveBalanceDto> list(Long employeeId, int year) {
        User u = currentUserService.requireCurrentUser();
        if (!isHrOrAdmin(u)) {
            if (u.getEmployee() == null || !u.getEmployee().getId().equals(employeeId)) {
                throw new IllegalArgumentException("You can only view your own leave balances");
            }
        }
        if (!employeeRepository.existsById(employeeId)) {
            throw new IllegalArgumentException("Employee not found: " + employeeId);
        }
        return leaveBalanceRepository.findByEmployeeIdAndYear(employeeId, year).stream()
                .map(LeaveBalanceDto::from)
                .collect(Collectors.toList());
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
            b.setUsedDays(java.math.BigDecimal.ZERO);
        }
        if (req.allocatedDays().compareTo(b.getUsedDays()) < 0) {
            throw new IllegalArgumentException("Allocated days cannot be less than used days (" + b.getUsedDays() + ")");
        }
        b.setAllocatedDays(req.allocatedDays());
        return LeaveBalanceDto.from(leaveBalanceRepository.save(b));
    }

    private static boolean isHrOrAdmin(User u) {
        return u.getRoles().stream().anyMatch(r ->
                "ROLE_HR".equals(r.getName()) || "ROLE_ADMIN".equals(r.getName()));
    }
}
