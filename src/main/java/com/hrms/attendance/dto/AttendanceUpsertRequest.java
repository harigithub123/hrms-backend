package com.hrms.attendance.dto;

import com.hrms.attendance.AttendanceStatus;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

public record AttendanceUpsertRequest(
        @NotNull Long employeeId,
        @NotNull LocalDate workDate,
        LocalTime checkIn,
        LocalTime checkOut,
        @NotNull AttendanceStatus status,
        String notes
) {}
