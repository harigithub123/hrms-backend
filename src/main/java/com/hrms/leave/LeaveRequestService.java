package com.hrms.leave;

import com.hrms.auth.entity.User;
import com.hrms.leave.dto.LeaveCalendarEntryDto;
import com.hrms.leave.dto.LeaveCalendarRangeDto;
import com.hrms.leave.dto.LeaveDecisionRequest;
import com.hrms.leave.dto.LeaveRequestCreateRequest;
import com.hrms.leave.dto.LeaveRequestDto;
import com.hrms.leave.entity.LeaveBalance;
import com.hrms.leave.entity.LeaveRequest;
import com.hrms.leave.repository.LeaveBalanceRepository;
import com.hrms.leave.repository.LeaveRequestRepository;
import com.hrms.leave.repository.LeaveTypeRepository;
import com.hrms.org.entity.Employee;
import com.hrms.org.repository.EmployeeRepository;
import com.hrms.security.CurrentUserService;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class LeaveRequestService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final EmployeeRepository employeeRepository;
    private final CurrentUserService currentUserService;

    public LeaveRequestService(
            LeaveRequestRepository leaveRequestRepository,
            LeaveBalanceRepository leaveBalanceRepository,
            LeaveTypeRepository leaveTypeRepository,
            EmployeeRepository employeeRepository,
            CurrentUserService currentUserService
    ) {
        this.leaveRequestRepository = leaveRequestRepository;
        this.leaveBalanceRepository = leaveBalanceRepository;
        this.leaveTypeRepository = leaveTypeRepository;
        this.employeeRepository = employeeRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public List<LeaveRequestDto> list(Long filterEmployeeId, LeaveRequestStatus status) {
        User u = currentUserService.requireCurrentUser();
        Specification<LeaveRequest> spec = buildVisibilitySpec(u, filterEmployeeId, status);
        return leaveRequestRepository.findAll(spec).stream()
                .sorted((a, b) -> b.getRequestedAt().compareTo(a.getRequestedAt()))
                .map(LeaveRequestDto::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<LeaveRequestDto> listPendingForApprover() {
        User u = currentUserService.requireCurrentUser();
        if (isHrOrAdmin(u)) {
            return leaveRequestRepository.findAll((root, q, cb) -> cb.equal(root.get("status"), LeaveRequestStatus.PENDING))
                    .stream()
                    .sorted((a, b) -> b.getRequestedAt().compareTo(a.getRequestedAt()))
                    .map(LeaveRequestDto::from)
                    .collect(Collectors.toList());
        }
        if (u.getEmployee() == null) {
            return List.of();
        }
        List<Long> team = employeeRepository.findByManagerId(u.getEmployee().getId()).stream()
                .map(Employee::getId)
                .collect(Collectors.toList());
        if (team.isEmpty()) {
            return List.of();
        }
        return leaveRequestRepository.findByStatusAndEmployeeIdIn(LeaveRequestStatus.PENDING, team).stream()
                .sorted((a, b) -> b.getRequestedAt().compareTo(a.getRequestedAt()))
                .map(LeaveRequestDto::from)
                .collect(Collectors.toList());
    }

    /**
     * One row per leave request overlapping the window (recommended for month view — no duplicated rows per day).
     */
    @Transactional(readOnly = true)
    public List<LeaveCalendarRangeDto> calendarRanges(LocalDate from, LocalDate to, Long employeeId) {
        validateCalendarWindow(from, to);
        User u = currentUserService.requireCurrentUser();
        assertCalendarAccess(u);
        List<LeaveRequestStatus> statuses = List.of(LeaveRequestStatus.APPROVED, LeaveRequestStatus.PENDING);
        List<LeaveRequest> requests = leaveRequestRepository.findOverlappingForCalendar(from, to, statuses, employeeId);
        List<LeaveCalendarRangeDto> out = new ArrayList<>();
        for (LeaveRequest r : requests) {
            if (!isLeaveVisibleToCurrentUser(u, r)) {
                continue;
            }
            String name = (r.getEmployee().getFirstName() + " " + r.getEmployee().getLastName()).trim();
            out.add(new LeaveCalendarRangeDto(
                    r.getId(),
                    r.getEmployee().getId(),
                    name,
                    r.getLeaveType().getId(),
                    r.getLeaveType().getCode(),
                    r.getLeaveType().getName(),
                    r.getStartDate(),
                    r.getEndDate(),
                    r.getTotalDays(),
                    r.getStatus()
            ));
        }
        return out;
    }

    /**
     * One row per calendar day (legacy / detailed heatmaps). Prefer {@link #calendarRanges} for month grids.
     */
    @Transactional(readOnly = true)
    public List<LeaveCalendarEntryDto> calendarDays(LocalDate from, LocalDate to, Long employeeId) {
        validateCalendarWindow(from, to);
        User u = currentUserService.requireCurrentUser();
        assertCalendarAccess(u);
        List<LeaveRequestStatus> statuses = List.of(LeaveRequestStatus.APPROVED, LeaveRequestStatus.PENDING);
        List<LeaveRequest> requests = leaveRequestRepository.findOverlappingForCalendar(from, to, statuses, employeeId);
        List<LeaveCalendarEntryDto> out = new ArrayList<>();
        for (LeaveRequest r : requests) {
            if (!isLeaveVisibleToCurrentUser(u, r)) {
                continue;
            }
            LocalDate d = r.getStartDate();
            while (!d.isAfter(r.getEndDate())) {
                if (!d.isBefore(from) && !d.isAfter(to)) {
                    String name = (r.getEmployee().getFirstName() + " " + r.getEmployee().getLastName()).trim();
                    out.add(new LeaveCalendarEntryDto(
                            d,
                            r.getId(),
                            r.getEmployee().getId(),
                            name,
                            r.getLeaveType().getId(),
                            r.getLeaveType().getCode(),
                            r.getLeaveType().getName(),
                            r.getStatus()
                    ));
                }
                d = d.plusDays(1);
            }
        }
        return out;
    }

    private static void validateCalendarWindow(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("from and to are required");
        }
        if (to.isBefore(from)) {
            throw new IllegalArgumentException("to must be on or after from");
        }
    }

    private void assertCalendarAccess(User u) {
        if (!isHrOrAdmin(u) && u.getEmployee() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Calendar requires HR/Admin or a linked employee profile");
        }
    }

    /** HR/Admin see all; others only self or direct reports. */
    private boolean isLeaveVisibleToCurrentUser(User u, LeaveRequest r) {
        if (isHrOrAdmin(u)) {
            return true;
        }
        if (u.getEmployee() == null) {
            return false;
        }
        boolean self = r.getEmployee().getId().equals(u.getEmployee().getId());
        boolean report = r.getEmployee().getManager() != null
                && r.getEmployee().getManager().getId().equals(u.getEmployee().getId());
        return self || report;
    }

    @Transactional
    public LeaveRequestDto create(LeaveRequestCreateRequest req) {
        User u = currentUserService.requireCurrentUser();
        Long targetEmpId = resolveTargetEmployeeId(u, req.employeeId());
        if (req.endDate().isBefore(req.startDate())) {
            throw new IllegalArgumentException("endDate must be on or after startDate");
        }
        long inclusiveDays = ChronoUnit.DAYS.between(req.startDate(), req.endDate()) + 1;
        BigDecimal totalDays = BigDecimal.valueOf(inclusiveDays);

        var leaveType = leaveTypeRepository.findById(req.leaveTypeId())
                .orElseThrow(() -> new IllegalArgumentException("Leave type not found: " + req.leaveTypeId()));
        if (!leaveType.isActive()) {
            throw new IllegalArgumentException("Leave type is inactive");
        }

        int year = req.startDate().getYear();
        LeaveBalance bal = leaveBalanceRepository.findByEmployeeIdAndLeaveTypeIdAndYear(targetEmpId, leaveType.getId(), year)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No leave balance for " + leaveType.getCode() + " in " + year + ". Ask HR to allocate."));
        BigDecimal pool = bal.getAllocatedDays().add(bal.getCarryForwardedDays());
        BigDecimal available = pool.subtract(bal.getUsedDays());
        if (available.compareTo(totalDays) < 0) {
            throw new IllegalArgumentException("Insufficient balance. Available: " + available);
        }

        LeaveRequest r = new LeaveRequest();
        r.setEmployee(employeeRepository.getReferenceById(targetEmpId));
        r.setLeaveType(leaveType);
        r.setStartDate(req.startDate());
        r.setEndDate(req.endDate());
        r.setTotalDays(totalDays);
        r.setReason(req.reason());
        r.setStatus(LeaveRequestStatus.PENDING);
        return LeaveRequestDto.from(leaveRequestRepository.save(r));
    }

    @Transactional
    public LeaveRequestDto decide(Long id, LeaveDecisionRequest decision) {
        User u = currentUserService.requireCurrentUser();
        LeaveRequest r = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Leave request not found: " + id));
        if (r.getStatus() != LeaveRequestStatus.PENDING) {
            throw new IllegalArgumentException("Leave request is not pending");
        }
        if (!canApprove(u, r)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot approve/reject this request");
        }
        if (Boolean.TRUE.equals(decision.approve())) {
            int year = r.getStartDate().getYear();
            LeaveBalance bal = leaveBalanceRepository
                    .findByEmployeeIdAndLeaveTypeIdAndYear(r.getEmployee().getId(), r.getLeaveType().getId(), year)
                    .orElseThrow(() -> new IllegalArgumentException("No leave balance for this request"));
            BigDecimal newUsed = bal.getUsedDays().add(r.getTotalDays());
            BigDecimal pool = bal.getAllocatedDays().add(bal.getCarryForwardedDays());
            if (newUsed.compareTo(pool) > 0) {
                throw new IllegalArgumentException("Approval would exceed leave balance (allocated + carry-forward)");
            }
            bal.setUsedDays(newUsed);
            leaveBalanceRepository.save(bal);
            r.setStatus(LeaveRequestStatus.APPROVED);
        } else {
            r.setStatus(LeaveRequestStatus.REJECTED);
        }
        r.setDecidedAt(java.time.Instant.now());
        r.setDecidedBy(u);
        r.setDecisionComment(decision.comment());
        return LeaveRequestDto.from(leaveRequestRepository.save(r));
    }

    private Specification<LeaveRequest> buildVisibilitySpec(User u, Long filterEmployeeId, LeaveRequestStatus status) {
        return (root, query, cb) -> {
            List<Predicate> preds = new ArrayList<>();
            List<Long> visible = visibleEmployeeIds(u);
            if (visible != null) {
                if (visible.isEmpty()) {
                    return cb.disjunction();
                }
                preds.add(root.get("employee").get("id").in(visible));
            }
            if (filterEmployeeId != null) {
                if (visible != null && !visible.contains(filterEmployeeId)) {
                    return cb.disjunction();
                }
                preds.add(cb.equal(root.get("employee").get("id"), filterEmployeeId));
            }
            if (status != null) {
                preds.add(cb.equal(root.get("status"), status));
            }
            return preds.isEmpty() ? cb.conjunction() : cb.and(preds.toArray(Predicate[]::new));
        };
    }

    /** null = HR/Admin sees all */
    private List<Long> visibleEmployeeIds(User u) {
        if (isHrOrAdmin(u)) {
            return null;
        }
        List<Long> ids = new ArrayList<>();
        if (u.getEmployee() != null) {
            ids.add(u.getEmployee().getId());
            ids.addAll(employeeRepository.findByManagerId(u.getEmployee().getId()).stream()
                    .map(Employee::getId)
                    .toList());
        }
        return ids;
    }

    private Long resolveTargetEmployeeId(User u, Long requestedEmployeeId) {
        if (isHrOrAdmin(u) && requestedEmployeeId != null) {
            if (!employeeRepository.existsById(requestedEmployeeId)) {
                throw new IllegalArgumentException("Employee not found: " + requestedEmployeeId);
            }
            return requestedEmployeeId;
        }
        if (u.getEmployee() == null) {
            throw new IllegalArgumentException("No employee linked to your user. Ask HR to link your login to an employee.");
        }
        if (requestedEmployeeId != null && !Objects.equals(requestedEmployeeId, u.getEmployee().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only create leave for yourself");
        }
        return u.getEmployee().getId();
    }

    private boolean canApprove(User u, LeaveRequest r) {
        if (isHrOrAdmin(u)) {
            return true;
        }
        if (u.getEmployee() == null) {
            return false;
        }
        if (r.getEmployee().getManager() == null) {
            return false;
        }
        return u.getEmployee().getId().equals(r.getEmployee().getManager().getId());
    }

    private static boolean isHrOrAdmin(User u) {
        return u.getRoles().stream().anyMatch(role ->
                "ROLE_HR".equals(role.getName()) || "ROLE_ADMIN".equals(role.getName()));
    }
}
