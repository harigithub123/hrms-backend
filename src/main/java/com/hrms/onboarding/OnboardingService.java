package com.hrms.onboarding;

import com.hrms.auth.entity.User;
import com.hrms.auth.repository.UserRepository;
import com.hrms.leave.entity.LeaveBalance;
import com.hrms.leave.entity.LeaveType;
import com.hrms.leave.repository.LeaveBalanceRepository;
import com.hrms.leave.repository.LeaveTypeRepository;
import com.hrms.offers.entity.JobOffer;
import com.hrms.offers.repository.JobOfferRepository;
import com.hrms.onboarding.dto.*;
import com.hrms.onboarding.entity.OnboardingCase;
import com.hrms.onboarding.entity.OnboardingTask;
import com.hrms.onboarding.repository.OnboardingCaseRepository;
import com.hrms.onboarding.repository.OnboardingTaskRepository;
import com.hrms.org.entity.Employee;
import com.hrms.org.repository.DepartmentRepository;
import com.hrms.org.repository.DesignationRepository;
import com.hrms.org.repository.EmployeeRepository;
import com.hrms.security.CurrentUserService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Year;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OnboardingService {

    private static final List<String> DEFAULT_TASKS = List.of(
            "Collect signed documents",
            "IT access & equipment",
            "Payroll & bank details",
            "Orientation schedule"
    );

    private final OnboardingCaseRepository caseRepository;
    private final OnboardingTaskRepository taskRepository;
    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final DesignationRepository designationRepository;
    private final JobOfferRepository jobOfferRepository;
    private final UserRepository userRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final CurrentUserService currentUserService;

    public OnboardingService(
            OnboardingCaseRepository caseRepository,
            OnboardingTaskRepository taskRepository,
            EmployeeRepository employeeRepository,
            DepartmentRepository departmentRepository,
            DesignationRepository designationRepository,
            JobOfferRepository jobOfferRepository,
            UserRepository userRepository,
            LeaveTypeRepository leaveTypeRepository,
            LeaveBalanceRepository leaveBalanceRepository,
            CurrentUserService currentUserService
    ) {
        this.caseRepository = caseRepository;
        this.taskRepository = taskRepository;
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
        this.designationRepository = designationRepository;
        this.jobOfferRepository = jobOfferRepository;
        this.userRepository = userRepository;
        this.leaveTypeRepository = leaveTypeRepository;
        this.leaveBalanceRepository = leaveBalanceRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public List<OnboardingCaseDto> list() {
        requireHrAdmin();
        return caseRepository.findAllByOrderByIdDesc().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OnboardingCaseDto get(Long id) {
        requireHrAdmin();
        OnboardingCase c = caseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Onboarding case not found: " + id));
        return toDto(c);
    }

    @Transactional
    public OnboardingCaseDto create(OnboardingCreateRequest req) {
        requireHrAdmin();
        OnboardingCase c = new OnboardingCase();
        c.setStatus(OnboardingStatus.DRAFT);
        c.setCandidateFirstName(req.candidateFirstName().trim());
        c.setCandidateLastName(req.candidateLastName().trim());
        c.setCandidateEmail(req.candidateEmail());
        c.setJoinDate(req.joinDate());
        c.setDepartment(req.departmentId() != null ? departmentRepository.getReferenceById(req.departmentId()) : null);
        c.setDesignation(req.designationId() != null ? designationRepository.getReferenceById(req.designationId()) : null);
        c.setManager(req.managerId() != null ? employeeRepository.getReferenceById(req.managerId()) : null);
        c.setOffer(req.offerId() != null ? jobOfferRepository.getReferenceById(req.offerId()) : null);
        c.setAssignedHr(req.assignedHrUserId() != null ? userRepository.getReferenceById(req.assignedHrUserId()) : null);
        c.setNotes(req.notes());
        c = caseRepository.save(c);
        int order = 0;
        for (String label : DEFAULT_TASKS) {
            OnboardingTask t = new OnboardingTask();
            t.setOnboardingCase(c);
            t.setLabel(label);
            t.setDone(false);
            t.setSortOrder(order++);
            taskRepository.save(t);
        }
        return toDto(caseRepository.findById(c.getId()).orElseThrow());
    }

    @Transactional
    public OnboardingCaseDto updateStatus(Long id, OnboardingStatus status) {
        requireHrAdmin();
        OnboardingCase c = caseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Onboarding case not found: " + id));
        c.setStatus(status);
        return toDto(caseRepository.save(c));
    }

    @Transactional
    public OnboardingTaskDto updateTask(Long caseId, Long taskId, OnboardingTaskUpdateRequest req) {
        requireHrAdmin();
        OnboardingTask t = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));
        if (!t.getOnboardingCase().getId().equals(caseId)) {
            throw new IllegalArgumentException("Task does not belong to case");
        }
        t.setDone(req.done());
        return OnboardingTaskDto.from(taskRepository.save(t));
    }

    @Transactional
    public OnboardingCaseDto complete(Long id) {
        requireHrAdmin();
        OnboardingCase c = caseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Onboarding case not found: " + id));
        if (c.getStatus() == OnboardingStatus.COMPLETED) {
            throw new IllegalArgumentException("Already completed");
        }
        if (c.getEmployee() != null) {
            throw new IllegalArgumentException("Employee already linked");
        }

        Employee e = new Employee();
        e.setFirstName(c.getCandidateFirstName());
        e.setLastName(c.getCandidateLastName());
        e.setEmail(c.getCandidateEmail());
        e.setDepartment(c.getDepartment());
        e.setDesignation(c.getDesignation());
        e.setManager(c.getManager());
        e.setJoinedAt(c.getJoinDate());
        if (c.getOffer() != null) {
            JobOffer o = jobOfferRepository.findById(c.getOffer().getId()).orElse(null);
            if (o != null && o.getCandidateEmail() != null && !o.getCandidateEmail().isBlank()) {
                e.setEmail(o.getCandidateEmail());
            }
        }
        final Employee saved = employeeRepository.save(e);
        c.setEmployee(saved);
        c.setStatus(OnboardingStatus.COMPLETED);
        caseRepository.save(c);

        final int year = c.getJoinDate() != null ? c.getJoinDate().getYear() : Year.now().getValue();
        for (LeaveType lt : leaveTypeRepository.findByActiveTrueOrderByNameAsc()) {
            LeaveBalance b = leaveBalanceRepository
                    .findByEmployeeIdAndLeaveTypeIdAndYear(saved.getId(), lt.getId(), year)
                    .orElseGet(() -> {
                        LeaveBalance nb = new LeaveBalance();
                        nb.setEmployee(saved);
                        nb.setLeaveType(lt);
                        nb.setYear(year);
                        nb.setUsedDays(BigDecimal.ZERO);
                        return nb;
                    });
            b.setAllocatedDays(lt.getDaysPerYear());
            leaveBalanceRepository.save(b);
        }

        return toDto(caseRepository.findById(id).orElseThrow());
    }

    private OnboardingCaseDto toDto(OnboardingCase c) {
        List<OnboardingTaskDto> tasks = taskRepository.findByOnboardingCaseIdOrderBySortOrderAsc(c.getId()).stream()
                .map(OnboardingTaskDto::from)
                .toList();
        return OnboardingCaseDto.from(c, tasks);
    }

    private void requireHrAdmin() {
        User u = currentUserService.requireCurrentUser();
        if (!isHrOrAdmin(u)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "HR or Admin only");
        }
    }

    private static boolean isHrOrAdmin(User u) {
        return u.getRoles().stream().anyMatch(r ->
                "ROLE_HR".equals(r.getName()) || "ROLE_ADMIN".equals(r.getName()));
    }
}
