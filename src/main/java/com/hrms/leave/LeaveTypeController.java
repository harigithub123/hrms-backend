package com.hrms.leave;

import com.hrms.leave.dto.LeaveTypeDto;
import com.hrms.leave.dto.LeaveTypeRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leave/types")
public class LeaveTypeController {

    private final LeaveTypeService leaveTypeService;

    public LeaveTypeController(LeaveTypeService leaveTypeService) {
        this.leaveTypeService = leaveTypeService;
    }

    @GetMapping("/active")
    @PreAuthorize("isAuthenticated()")
    public List<LeaveTypeDto> listActive() {
        return leaveTypeService.findAllActive();
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public List<LeaveTypeDto> listAll() {
        return leaveTypeService.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public LeaveTypeDto get(@PathVariable Long id) {
        return leaveTypeService.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<LeaveTypeDto> create(@Valid @RequestBody LeaveTypeRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(leaveTypeService.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public LeaveTypeDto update(@PathVariable Long id, @Valid @RequestBody LeaveTypeRequest req) {
        return leaveTypeService.update(id, req);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        leaveTypeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
