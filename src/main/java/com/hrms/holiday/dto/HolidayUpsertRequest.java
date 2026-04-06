package com.hrms.holiday.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record HolidayUpsertRequest(
        @NotNull LocalDate holidayDate,
        @NotBlank @Size(max = 200) String name
) {
}
