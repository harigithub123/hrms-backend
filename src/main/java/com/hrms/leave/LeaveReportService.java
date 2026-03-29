package com.hrms.leave;

import com.hrms.auth.entity.User;
import com.hrms.leave.dto.LeaveLedgerRowDto;
import com.hrms.leave.entity.LeaveBalance;
import com.hrms.leave.entity.LeaveBalanceAdjustment;
import com.hrms.leave.entity.LeaveRequest;
import com.hrms.leave.repository.LeaveBalanceAdjustmentRepository;
import com.hrms.leave.repository.LeaveBalanceRepository;
import com.hrms.leave.repository.LeaveRequestRepository;
import com.hrms.org.repository.EmployeeRepository;
import com.hrms.security.CurrentUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class LeaveReportService {

    private static final ZoneId REPORT_ZONE = ZoneId.systemDefault();

    private final LeaveBalanceRepository leaveBalanceRepository;
    private final LeaveBalanceAdjustmentRepository adjustmentRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final EmployeeRepository employeeRepository;
    private final CurrentUserService currentUserService;

    public LeaveReportService(
            LeaveBalanceRepository leaveBalanceRepository,
            LeaveBalanceAdjustmentRepository adjustmentRepository,
            LeaveRequestRepository leaveRequestRepository,
            EmployeeRepository employeeRepository,
            CurrentUserService currentUserService
    ) {
        this.leaveBalanceRepository = leaveBalanceRepository;
        this.adjustmentRepository = adjustmentRepository;
        this.leaveRequestRepository = leaveRequestRepository;
        this.employeeRepository = employeeRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public List<LeaveLedgerRowDto> ledger(Long employeeId, int year) {
        assertCanView(employeeId);
        if (!employeeRepository.existsById(employeeId)) {
            throw new IllegalArgumentException("Employee not found: " + employeeId);
        }

        LocalDate yStart = LocalDate.of(year, 1, 1);
        LocalDate yEnd = LocalDate.of(year, 12, 31);
        Instant yearStartInstant = yStart.atStartOfDay(REPORT_ZONE).toInstant();

        List<LeaveBalanceAdjustment> adjustments = adjustmentRepository.findHistoryForEmployeeYear(employeeId, year);
        List<LeaveRequest> approvedLeaves = leaveRequestRepository.findApprovedForLedger(
                employeeId, LeaveRequestStatus.APPROVED, yStart, yEnd);

        Set<Long> typeIds = new HashSet<>();
        leaveBalanceRepository.findByEmployeeIdAndYear(employeeId, year).forEach(b -> typeIds.add(b.getLeaveType().getId()));
        for (LeaveBalanceAdjustment a : adjustments) {
            typeIds.add(a.getLeaveBalance().getLeaveType().getId());
        }
        for (LeaveRequest r : approvedLeaves) {
            typeIds.add(r.getLeaveType().getId());
        }

        List<SortableRow> merged = new ArrayList<>();
        for (Long typeId : typeIds) {
            merged.addAll(buildRowsForType(employeeId, year, typeId, yearStartInstant, yStart, adjustments, approvedLeaves));
        }
        merged.sort(Comparator
                .comparingLong(SortableRow::sortKey)
                .thenComparing(r -> r.dto.leaveTypeCode())
                .thenComparingLong(SortableRow::tieId));

        return merged.stream().map(SortableRow::dto).toList();
    }

    private List<SortableRow> buildRowsForType(
            Long employeeId,
            int year,
            Long leaveTypeId,
            Instant yearStartInstant,
            LocalDate yStart,
            List<LeaveBalanceAdjustment> allAdjustments,
            List<LeaveRequest> allLeaves
    ) {
        List<LeaveBalanceAdjustment> adjs = allAdjustments.stream()
                .filter(a -> a.getLeaveBalance().getLeaveType().getId().equals(leaveTypeId))
                .toList();
        List<LeaveRequest> leaves = allLeaves.stream()
                .filter(r -> r.getLeaveType().getId().equals(leaveTypeId))
                .toList();

        LeaveBalance bal = leaveBalanceRepository
                .findByEmployeeIdAndLeaveTypeIdAndYear(employeeId, leaveTypeId, year)
                .orElse(null);

        if (adjs.isEmpty() && leaves.isEmpty()) {
            if (bal == null) {
                return List.of();
            }
            BigDecimal avail = bal.getAllocatedDays().add(bal.getCarryForwardedDays()).subtract(bal.getUsedDays());
            return List.of(new SortableRow(
                    yearStartInstant.toEpochMilli() * 1_000_000L,
                    -1L,
                    new LeaveLedgerRowDto(
                            yStart.toString(),
                            bal.getLeaveType().getCode(),
                            bal.getLeaveType().getName(),
                            "OPENING",
                            null,
                            avail.stripTrailingZeros(),
                            "No movements recorded this year; balance shown is current."
                    )
            ));
        }

        BigDecimal currentAvail = bal == null
                ? BigDecimal.ZERO
                : bal.getAllocatedDays().add(bal.getCarryForwardedDays()).subtract(bal.getUsedDays());

        List<LedgerEvent> events = new ArrayList<>();
        for (LeaveBalanceAdjustment a : adjs) {
            BigDecimal impact = switch (a.getKind()) {
                case ALLOCATION, CARRY_FORWARD -> a.getDeltaDays();
                case LAPSE -> a.getDeltaDays().negate();
            };
            BigDecimal dayMag = a.getDeltaDays().abs();
            String action = switch (a.getKind()) {
                case ALLOCATION -> "ALLOCATED";
                case CARRY_FORWARD -> "CARRY_FORWARD";
                case LAPSE -> "LAPSE";
            };
            LocalDate display = a.getCreatedAt().atZone(REPORT_ZONE).toLocalDate();
            long sortKey = a.getCreatedAt().toEpochMilli() * 1_000_000L + a.getId();
            events.add(new LedgerEvent(
                    sortKey,
                    display,
                    action,
                    impact,
                    dayMag,
                    a.getCommentText(),
                    a.getId()
            ));
        }
        for (LeaveRequest r : leaves) {
            BigDecimal td = r.getTotalDays();
            Instant when = r.getDecidedAt();
            LocalDate display = r.getStartDate();
            long sortKey = when.toEpochMilli() * 1_000_000L + r.getId();
            events.add(new LedgerEvent(
                    sortKey,
                    display,
                    "LEAVE_TAKEN",
                    td.negate(),
                    td,
                    r.getReason() != null && !r.getReason().isBlank() ? r.getReason().trim() : "Approved leave",
                    r.getId() + 5_000_000_000L
            ));
        }

        events.sort(Comparator.comparingLong(LedgerEvent::sortKey));

        BigDecimal avail = currentAvail;
        for (int i = events.size() - 1; i >= 0; i--) {
            avail = avail.subtract(events.get(i).impactOnAvailable);
        }
        BigDecimal opening = avail;

        List<SortableRow> rows = new ArrayList<>();
        rows.add(new SortableRow(
                yearStartInstant.toEpochMilli() * 1_000_000L,
                -1L,
                new LeaveLedgerRowDto(
                        yStart.toString(),
                        firstTypeCode(adjs, leaves),
                        firstTypeName(adjs, leaves),
                        "OPENING",
                        null,
                        opening.stripTrailingZeros(),
                        "Balance at start of year (derived from current ledger)"
                )
        ));

        avail = opening;
        for (LedgerEvent e : events) {
            avail = avail.add(e.impactOnAvailable);
            rows.add(new SortableRow(
                    e.sortKey,
                    e.tieId,
                    new LeaveLedgerRowDto(
                            e.displayDate.toString(),
                            firstTypeCode(adjs, leaves),
                            firstTypeName(adjs, leaves),
                            e.action,
                            e.dayMagnitude.stripTrailingZeros(),
                            avail.stripTrailingZeros(),
                            truncate(e.details, 500)
                    )
            ));
        }
        return rows;
    }

    private static String firstTypeCode(List<LeaveBalanceAdjustment> adjs, List<LeaveRequest> leaves) {
        if (!adjs.isEmpty()) {
            return adjs.get(0).getLeaveBalance().getLeaveType().getCode();
        }
        return leaves.get(0).getLeaveType().getCode();
    }

    private static String firstTypeName(List<LeaveBalanceAdjustment> adjs, List<LeaveRequest> leaves) {
        if (!adjs.isEmpty()) {
            return adjs.get(0).getLeaveBalance().getLeaveType().getName();
        }
        return leaves.get(0).getLeaveType().getName();
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }

    private void assertCanView(Long employeeId) {
        User u = currentUserService.requireCurrentUser();
        if (!isHrOrAdmin(u)) {
            if (u.getEmployee() == null || !u.getEmployee().getId().equals(employeeId)) {
                throw new IllegalArgumentException("You can only view your own leave report");
            }
        }
    }

    private static boolean isHrOrAdmin(User u) {
        return u.getRoles().stream().anyMatch(r ->
                "ROLE_HR".equals(r.getName()) || "ROLE_ADMIN".equals(r.getName()));
    }

    private record LedgerEvent(
            long sortKey,
            LocalDate displayDate,
            String action,
            BigDecimal impactOnAvailable,
            BigDecimal dayMagnitude,
            String details,
            long tieId
    ) {}

    private record SortableRow(long sortKey, long tieId, LeaveLedgerRowDto dto) {}
}
