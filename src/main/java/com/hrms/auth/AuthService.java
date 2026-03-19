package com.hrms.auth;

import com.hrms.auth.entity.RefreshToken;
import com.hrms.auth.entity.User;
import com.hrms.auth.repository.RefreshTokenRepository;
import com.hrms.auth.repository.UserRepository;
import com.hrms.org.repository.EmployeeRepository;
import com.hrms.config.JwtProperties;
import com.hrms.security.JwtService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final EmployeeRepository employeeRepository;

    public AuthService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            JwtProperties jwtProperties,
            EmployeeRepository employeeRepository
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
        this.employeeRepository = employeeRepository;
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BadCredentialsException("Invalid username or password");
        }
        if (!user.isEnabled()) {
            throw new BadCredentialsException("User account is disabled");
        }
        String accessToken = jwtService.generateAccessToken(user);
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setUser(user);
        refreshToken.setExpiresAt(Instant.now().plusSeconds(jwtProperties.getRefreshTokenValidityDays() * 86400L));
        refreshTokenRepository.save(refreshToken);

        long expiresIn = jwtProperties.getAccessTokenValidityMinutes() * 60L;
        return new AuthResponse(
                accessToken,
                refreshToken.getToken(),
                expiresIn,
                AuthResponse.TOKEN_TYPE,
                toUserInfo(user)
        );
    }

    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        RefreshToken stored = refreshTokenRepository.findByToken(request.refreshToken())
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));
        if (stored.isRevoked() || stored.getExpiresAt().isBefore(Instant.now())) {
            stored.setRevoked(true);
            refreshTokenRepository.save(stored);
            throw new BadCredentialsException("Invalid or expired refresh token");
        }
        User user = stored.getUser();
        userRepository.findById(user.getId()).orElseThrow(() -> new BadCredentialsException("User not found"));
        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        String accessToken = jwtService.generateAccessToken(user);
        RefreshToken newRefresh = new RefreshToken();
        newRefresh.setToken(UUID.randomUUID().toString());
        newRefresh.setUser(user);
        newRefresh.setExpiresAt(Instant.now().plusSeconds(jwtProperties.getRefreshTokenValidityDays() * 86400L));
        refreshTokenRepository.save(newRefresh);

        long expiresIn = jwtProperties.getAccessTokenValidityMinutes() * 60L;
        return new AuthResponse(
                accessToken,
                newRefresh.getToken(),
                expiresIn,
                AuthResponse.TOKEN_TYPE,
                toUserInfo(user)
        );
    }

    public AuthResponse.UserInfo getCurrentUserInfo(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException("User not found"));
        return toUserInfo(user);
    }

    @Transactional(readOnly = true)
    public List<UserSummaryDto> listAllUsersForHr() {
        return userRepository.findAll().stream()
                .map(u -> new UserSummaryDto(
                        u.getId(),
                        u.getUsername(),
                        u.getEmployee() != null ? u.getEmployee().getId() : null
                ))
                .collect(Collectors.toList());
    }

    private AuthResponse.UserInfo toUserInfo(User user) {
        List<String> roles = user.getRoles().stream()
                .map(com.hrms.auth.entity.Role::getName)
                .collect(Collectors.toList());
        Long employeeId = user.getEmployee() != null ? user.getEmployee().getId() : null;
        int directReportCount = 0;
        if (employeeId != null) {
            directReportCount = (int) employeeRepository.countByManagerId(employeeId);
        }
        return new AuthResponse.UserInfo(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                roles,
                employeeId,
                directReportCount
        );
    }

    @Transactional
    public void linkUserToEmployee(Long userId, Long employeeId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        if (employeeId == null) {
            user.setEmployee(null);
        } else {
            if (!employeeRepository.existsById(employeeId)) {
                throw new IllegalArgumentException("Employee not found: " + employeeId);
            }
            user.setEmployee(employeeRepository.getReferenceById(employeeId));
        }
        userRepository.save(user);
    }
}
