package com.hrms.holiday;

import com.hrms.holiday.dto.HolidayDto;
import com.hrms.holiday.dto.HolidayUpsertRequest;
import com.hrms.holiday.entity.Holiday;
import com.hrms.holiday.repository.HolidayRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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

    @Transactional
    public HolidayDto create(HolidayUpsertRequest req) {
        if (holidayRepository.existsByHolidayDate(req.holidayDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A holiday already exists on this date");
        }
        Holiday h = new Holiday();
        h.setHolidayDate(req.holidayDate());
        h.setName(req.name().trim());
        return HolidayDto.from(holidayRepository.save(h));
    }

    @Transactional
    public HolidayDto update(Long id, HolidayUpsertRequest req) {
        Holiday h = holidayRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Holiday not found: " + id));
        if (!h.getHolidayDate().equals(req.holidayDate())
                && holidayRepository.existsByHolidayDateAndIdNot(req.holidayDate(), id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A holiday already exists on this date");
        }
        h.setHolidayDate(req.holidayDate());
        h.setName(req.name().trim());
        return HolidayDto.from(holidayRepository.save(h));
    }

    @Transactional
    public void delete(Long id) {
        if (!holidayRepository.existsById(id)) {
            throw new IllegalArgumentException("Holiday not found: " + id);
        }
        holidayRepository.deleteById(id);
    }
}
