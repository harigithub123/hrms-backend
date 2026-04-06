package com.hrms.org;

import com.hrms.auth.EmployeeAccountService;
import com.hrms.auth.entity.User;
import com.hrms.org.entity.Department;
import com.hrms.org.entity.Designation;
import com.hrms.org.entity.Employee;
import com.hrms.onboarding.OnboardingService;
import com.hrms.org.repository.DepartmentRepository;
import com.hrms.org.repository.DesignationRepository;
import com.hrms.org.repository.EmployeeRepository;
import com.hrms.security.CurrentUserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final DesignationRepository designationRepository;
    private final CurrentUserService currentUserService;
    private final EmployeeAccountService employeeAccountService;
    private final OnboardingService onboardingService;

    public EmployeeService(EmployeeRepository employeeRepository,
                           DepartmentRepository departmentRepository,
                           DesignationRepository designationRepository,
                           CurrentUserService currentUserService,
                           EmployeeAccountService employeeAccountService,
                           OnboardingService onboardingService) {
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
        this.designationRepository = designationRepository;
        this.currentUserService = currentUserService;
        this.employeeAccountService = employeeAccountService;
        this.onboardingService = onboardingService;
    }

    @Transactional(readOnly = true)
    public Page<EmployeeDto> findAll(Pageable pageable, String q) {
        String trimmed = q == null ? "" : q.trim();
        Page<Employee> page = trimmed.isEmpty()
                ? employeeRepository.findAll(pageable)
                : employeeRepository.searchByText(trimmed, pageable);
        return page.map(EmployeeDto::from);
    }

    @Transactional(readOnly = true)
    public List<EmployeeDto> findAll() {
        return employeeRepository.findAll().stream().map(EmployeeDto::from).collect(Collectors.toList());
    }

    /**
     * Direct reports of the logged-in user's linked employee (manager → reportees).
     * Empty list if the user has no employee link or no reportees.
     */
    @Transactional(readOnly = true)
    public List<EmployeeDto> findDirectReportsForCurrentUser() {
        User u = currentUserService.requireCurrentUser();
        Employee manager = u.getEmployee();
        if (manager == null) {
            return List.of();
        }
        return employeeRepository.findByManagerId(manager.getId()).stream()
                .map(EmployeeDto::from)
                .sorted(Comparator.comparing(EmployeeDto::lastName, Comparator.nullsLast(String::compareToIgnoreCase))
                        .thenComparing(EmployeeDto::firstName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public EmployeeDto getById(Long id) {
        return employeeRepository.findById(id).map(EmployeeDto::from)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + id));
    }

    @Transactional
    public EmployeeDto create(EmployeeRequest req) {
        Employee e = new Employee();
        mapRequestToEntity(req, e);
        validateSeparationExitDetails(e);
        e = employeeRepository.save(e);
        employeeAccountService.provisionUserForNewEmployee(e);
        if (EmploymentStatus.isSeparation(e.getEmploymentStatus())) {
            onboardingService.ensureExitLetterTasks(e.getId());
        }
        return EmployeeDto.from(e);
    }

    @Transactional
    public EmployeeDto update(Long id, EmployeeRequest req) {
        Employee e = employeeRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Employee not found: " + id));
        EmploymentStatus previousStatus = e.getEmploymentStatus();
        mapRequestToEntity(req, e);
        validateSeparationExitDetails(e);
        Employee saved = employeeRepository.save(e);
        employeeAccountService.syncEmailFromEmployee(saved);
        if (EmploymentStatus.isSeparation(saved.getEmploymentStatus()) && !EmploymentStatus.isSeparation(previousStatus)) {
            onboardingService.ensureExitLetterTasks(saved.getId());
        }
        return EmployeeDto.from(saved);
    }

    private void mapRequestToEntity(EmployeeRequest req, Employee e) {
        e.setEmployeeCode(req.employeeCode());
        e.setFirstName(req.firstName());
        e.setLastName(req.lastName());
        e.setEmail(req.email());
        e.setMobileNumber(req.mobileNumber());
        e.setJoinedAt(req.joinedAt());
        e.setDepartment(req.departmentId() != null ? departmentRepository.getReferenceById(req.departmentId()) : null);
        e.setDesignation(req.designationId() != null ? designationRepository.getReferenceById(req.designationId()) : null);
        e.setManager(req.managerId() != null ? employeeRepository.getReferenceById(req.managerId()) : null);
        if (req.employmentStatus() != null) {
            e.setEmploymentStatus(req.employmentStatus());
        }
        if (req.lastWorkingDate() != null) {
            e.setLastWorkingDate(req.lastWorkingDate());
        }
        if (req.exitReason() != null) {
            String r = req.exitReason().trim();
            e.setExitReason(r.isEmpty() ? null : r);
        }
    }

    private static void validateSeparationExitDetails(Employee e) {
        if (!EmploymentStatus.isSeparation(e.getEmploymentStatus())) {
            return;
        }
        if (e.getLastWorkingDate() == null) {
            throw new IllegalArgumentException("Last working date is required when employment status is not JOINED.");
        }
        if (e.getExitReason() == null || e.getExitReason().isBlank()) {
            throw new IllegalArgumentException("Exit reason is required when employment status is not JOINED.");
        }
    }
}
