package com.hrms.org;

import com.hrms.auth.entity.User;
import com.hrms.org.entity.Department;
import com.hrms.org.entity.Designation;
import com.hrms.org.entity.Employee;
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

    public EmployeeService(EmployeeRepository employeeRepository,
                           DepartmentRepository departmentRepository,
                           DesignationRepository designationRepository,
                           CurrentUserService currentUserService) {
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
        this.designationRepository = designationRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public Page<EmployeeDto> findAll(Pageable pageable) {
        return employeeRepository.findAll(pageable).map(EmployeeDto::from);
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
        return EmployeeDto.from(employeeRepository.save(e));
    }

    @Transactional
    public EmployeeDto update(Long id, EmployeeRequest req) {
        Employee e = employeeRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Employee not found: " + id));
        mapRequestToEntity(req, e);
        return EmployeeDto.from(employeeRepository.save(e));
    }

    @Transactional
    public void delete(Long id) {
        if (!employeeRepository.existsById(id)) throw new IllegalArgumentException("Employee not found: " + id);
        employeeRepository.deleteById(id);
    }

    private void mapRequestToEntity(EmployeeRequest req, Employee e) {
        e.setEmployeeCode(req.employeeCode());
        e.setFirstName(req.firstName());
        e.setLastName(req.lastName());
        e.setEmail(req.email());
        e.setJoinedAt(req.joinedAt());
        e.setDepartment(req.departmentId() != null ? departmentRepository.getReferenceById(req.departmentId()) : null);
        e.setDesignation(req.designationId() != null ? designationRepository.getReferenceById(req.designationId()) : null);
        e.setManager(req.managerId() != null ? employeeRepository.getReferenceById(req.managerId()) : null);
    }
}
