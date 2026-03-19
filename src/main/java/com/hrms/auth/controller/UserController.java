package com.hrms.auth.controller;

import com.hrms.auth.AuthResponse;
import com.hrms.auth.AuthService;
import com.hrms.auth.LinkEmployeeRequest;
import com.hrms.auth.UserSummaryDto;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final AuthService authService;

    public UserController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'HR')")
    public ResponseEntity<AuthResponse.UserInfo> me(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(authService.getCurrentUserInfo(userDetails.getUsername()));
    }

    @PutMapping("/{id}/employee")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<Void> linkEmployee(
            @PathVariable Long id,
            @Valid @RequestBody LinkEmployeeRequest body
    ) {
        authService.linkUserToEmployee(id, body.employeeId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public List<UserSummaryDto> listUsers() {
        return authService.listAllUsersForHr();
    }
}
