package com.hrms.compensation;

import com.hrms.compensation.dto.CompensationCreateRequest;
import com.hrms.compensation.dto.CompensationDto;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/compensation")
@PreAuthorize("hasAnyRole('HR','ADMIN')")
public class CompensationController {

    private final CompensationService compensationService;

    public CompensationController(CompensationService compensationService) {
        this.compensationService = compensationService;
    }

    @GetMapping("/employee/{employeeId}")
    public List<CompensationDto> listForEmployee(@PathVariable Long employeeId) {
        return compensationService.listForEmployee(employeeId);
    }

    @GetMapping("/search")
    public List<CompensationDto> search(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate effectiveDate
    ) {
        return compensationService.search(employeeId, effectiveDate);
    }

    @PostMapping
    public CompensationDto create(@Valid @RequestBody CompensationCreateRequest req) {
        return compensationService.create(req);
    }
}
