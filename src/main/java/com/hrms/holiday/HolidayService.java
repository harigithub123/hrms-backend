package com.hrms.holiday;

import com.hrms.holiday.dto.HolidayDto;
import com.hrms.holiday.repository.HolidayRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class HolidayService {

    private final HolidayRepository holidayRepository;

    public HolidayService(HolidayRepository holidayRepository) {
        this.holidayRepository = holidayRepository;
    }

    @Transactional(readOnly = true)
    public List<HolidayDto> listByYear(int year) {
        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate endExclusive = LocalDate.of(year + 1, 1, 1);
        return holidayRepository.findByHolidayDateBetweenOrderByHolidayDateAsc(start, endExclusive).stream()
                .map(HolidayDto::from)
                .collect(Collectors.toList());
    }
}
