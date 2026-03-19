package com.hrms.holiday.dto;

import com.hrms.holiday.entity.Holiday;

import java.time.LocalDate;

public record HolidayDto(
        Long id,
        LocalDate holidayDate,
        String name
) {
    public static HolidayDto from(Holiday h) {
        return new HolidayDto(h.getId(), h.getHolidayDate(), h.getName());
    }
}
