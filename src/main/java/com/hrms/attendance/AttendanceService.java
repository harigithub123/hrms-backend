package com.hrms.attendance;

import com.hrms.attendance.dto.AttendanceRecordDto;
import com.hrms.attendance.dto.AttendanceUpsertRequest;
import com.hrms.attendance.entity.AttendanceRecord;
import com.hrms.attendance.repository.AttendanceRecordRepository;
import com.hrms.auth.entity.User;
import com.hrms.org.entity.Employee;
import com.hrms.org.repository.EmployeeRepository;
import com.hrms.security.CurrentUserService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class AttendanceService {

    private final AttendanceRecordRepository attendanceRecordRepository;
    private final EmployeeRepository employeeRepository;
    private final CurrentUserService currentUserService;

    public AttendanceService(
            AttendanceRecordRepository attendanceRecordRepository,
            EmployeeRepository employeeRepository,
            CurrentUserService currentUserService
    ) {
        this.attendanceRecordRepository = attendanceRecordRepository;
        this.employeeRepository = employeeRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public List<AttendanceRecordDto> list(Long employeeId, LocalDate from, LocalDate to) {
        User u = currentUserService.requireCurrentUser();
        if (!canViewEmployeeAttendance(u, employeeId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot view attendance for this employee");
        }
        if (!employeeRepository.existsById(employeeId)) {
            throw new IllegalArgumentException("Employee not found: " + employeeId);
        }
        return attendanceRecordRepository.findByEmployeeIdAndWorkDateBetweenOrderByWorkDateAsc(employeeId, from, to)
                .stream()
                .map(AttendanceRecordDto::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public AttendanceRecordDto upsert(AttendanceUpsertRequest req) {
        User u = currentUserService.requireCurrentUser();
        if (!isHrOrAdmin(u)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only HR/Admin can record attendance");
        }
        if (!employeeRepository.existsById(req.employeeId())) {
            throw new IllegalArgumentException("Employee not found: " + req.employeeId());
        }
        AttendanceRecord r = attendanceRecordRepository
                .findByEmployeeIdAndWorkDate(req.employeeId(), req.workDate())
                .orElseGet(AttendanceRecord::new);
        if (r.getId() == null) {
            r.setEmployee(employeeRepository.getReferenceById(req.employeeId()));
            r.setWorkDate(req.workDate());
        }
        r.setCheckIn(req.checkIn());
        r.setCheckOut(req.checkOut());
        r.setStatus(req.status());
        r.setNotes(req.notes());
        return AttendanceRecordDto.from(attendanceRecordRepository.save(r));
    }

    private boolean canViewEmployeeAttendance(User u, Long employeeId) {
        if (isHrOrAdmin(u)) {
            return true;
        }
        if (u.getEmployee() == null) {
            return false;
        }
        if (Objects.equals(u.getEmployee().getId(), employeeId)) {
            return true;
        }
        return employeeRepository.findById(employeeId)
                .map(emp -> emp.getManager() != null && Objects.equals(emp.getManager().getId(), u.getEmployee().getId()))
                .orElse(false);
    }

    private static boolean isHrOrAdmin(User u) {
        return u.getRoles().stream().anyMatch(r ->
                "ROLE_HR".equals(r.getName()) || "ROLE_ADMIN".equals(r.getName()));
    }
}
