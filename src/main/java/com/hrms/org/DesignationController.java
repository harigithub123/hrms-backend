package com.hrms.org;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/designations")
@PreAuthorize("hasAnyRole('HR', 'ADMIN')")
public class DesignationController {

    private final DesignationService service;

    public DesignationController(DesignationService service) {
        this.service = service;
    }

    @GetMapping
    public Page<DesignationDto> list(@PageableDefault(size = 10, sort = "name") Pageable pageable) {
        return service.findAll(pageable);
    }

    @GetMapping("/all")
    public List<DesignationDto> listAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public DesignationDto get(@PathVariable Long id) {
        return service.getById(id);
    }

    @PostMapping
    public ResponseEntity<DesignationDto> create(@Valid @RequestBody DesignationRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
    }

    @PutMapping("/{id}")
    public DesignationDto update(@PathVariable Long id, @Valid @RequestBody DesignationRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
