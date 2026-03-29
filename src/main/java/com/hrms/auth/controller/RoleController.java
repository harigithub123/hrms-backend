package com.hrms.auth.controller;

import com.hrms.auth.RoleDto;
import com.hrms.auth.entity.Role;
import com.hrms.auth.repository.RoleRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/roles")
public class RoleController {

    private final RoleRepository roleRepository;

    public RoleController(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<RoleDto> listRoles() {
        return roleRepository.findAll().stream()
                .sorted(Comparator.comparing(Role::getName))
                .map(r -> new RoleDto(r.getId(), r.getName()))
                .toList();
    }
}
