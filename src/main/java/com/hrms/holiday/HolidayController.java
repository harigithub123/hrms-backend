package com.hrms.holiday;

import com.hrms.holiday.dto.HolidayDto;
import com.hrms.holiday.dto.HolidayUpsertRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/holidays")
public class HolidayController {

    private final HolidayService holidayService;

    public HolidayController(HolidayService holidayService) {
        this.holidayService = holidayService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<HolidayDto> list(@RequestParam int year) {
        return holidayService.listByYear(year);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public HolidayDto create(@Valid @RequestBody HolidayUpsertRequest req) {
        return holidayService.create(req);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public HolidayDto update(@PathVariable Long id, @Valid @RequestBody HolidayUpsertRequest req) {
        return holidayService.update(id, req);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public void delete(@PathVariable Long id) {
        holidayService.delete(id);
    }
}
