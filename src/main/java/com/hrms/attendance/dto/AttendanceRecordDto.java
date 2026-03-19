package com.hrms.attendance.dto;

import com.hrms.attendance.AttendanceStatus;
import com.hrms.attendance.entity.AttendanceRecord;

import java.time.LocalDate;
import java.time.LocalTime;

public record AttendanceRecordDto(
        Long id,
        Long employeeId,
        String employeeName,
        LocalDate workDate,
        LocalTime checkIn,
        LocalTime checkOut,
        AttendanceStatus status,
        String notes
) {
    public static AttendanceRecordDto from(AttendanceRecord r) {
        String name = (r.getEmployee().getFirstName() + " " + r.getEmployee().getLastName()).trim();
        return new AttendanceRecordDto(
                r.getId(),
                r.getEmployee().getId(),
                name,
                r.getWorkDate(),
                r.getCheckIn(),
                r.getCheckOut(),
                r.getStatus(),
                r.getNotes()
        );
    }
}
