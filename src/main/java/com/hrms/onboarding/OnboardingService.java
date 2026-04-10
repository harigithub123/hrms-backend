package com.hrms.onboarding;

import com.hrms.auth.entity.User;
import com.hrms.auth.repository.UserRepository;
import com.hrms.offers.entity.JobOffer;
import com.hrms.offers.repository.JobOfferRepository;
import com.hrms.onboarding.dto.*;
import com.hrms.onboarding.entity.OnboardingCase;
import com.hrms.onboarding.repository.OnboardingCaseRepository;
import com.hrms.org.EmploymentStatus;
import com.hrms.org.entity.Employee;
import com.hrms.org.repository.DepartmentRepository;
import com.hrms.org.repository.DesignationRepository;
import com.hrms.org.repository.EmployeeRepository;
import com.hrms.security.CurrentUserService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class OnboardingService {

    private final OnboardingCaseRepository caseRepository;
    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final DesignationRepository designationRepository;
    private final JobOfferRepository jobOfferRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final OnboardingHireCompletionService hireCompletionService;
    private final OnboardingTaskLifecycleService taskLifecycleService;
    private final OnboardingBankDetailsCommandService bankDetailsCommandService;

    public OnboardingService(
            OnboardingCaseRepository caseRepository,
            EmployeeRepository employeeRepository,
            DepartmentRepository departmentRepository,
            DesignationRepository designationRepository,
            JobOfferRepository jobOfferRepository,
            UserRepository userRepository,
            CurrentUserService currentUserService,
            OnboardingHireCompletionService hireCompletionService,
            OnboardingTaskLifecycleService taskLifecycleService,
            OnboardingBankDetailsCommandService bankDetailsCommandService
    ) {
        this.caseRepository = caseRepository;
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
        this.designationRepository = designationRepository;
        this.jobOfferRepository = jobOfferRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
        this.hireCompletionService = hireCompletionService;
        this.taskLifecycleService = taskLifecycleService;
        this.bankDetailsCommandService = bankDetailsCommandService;
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

    /**
     * Onboarding cases linked to employees whose employment status is {@link EmploymentStatus#RESIGNED} or
     * {@link EmploymentStatus#EXITED}. Use {@link #syncSeparationBoardCases()} to create missing cases and letter tasks.
     */
    @Transactional(readOnly = true)
    public List<OnboardingCaseDto> listSeparationBoard() {
        requireHrAdmin();
        return buildSeparationBoardList();
    }

    /**
     * Ensures each resigned or exited employee has a separation case with relieving, experience, and full &amp; final letter tasks.
     */
    @Transactional
    public List<OnboardingCaseDto> syncSeparationBoardCases() {
        requireHrAdmin();
        for (Employee e : employeeRepository.findByEmploymentStatusIn(
                EnumSet.of(EmploymentStatus.RESIGNED, EmploymentStatus.EXITED))) {
            ensureExitLetterTasks(e.getId());
        }
        return buildSeparationBoardList();
    }

    private List<OnboardingCaseDto> buildSeparationBoardList() {
        List<Employee> exits = employeeRepository.findByEmploymentStatusIn(
                EnumSet.of(EmploymentStatus.RESIGNED, EmploymentStatus.EXITED));
        return exits.stream()
                .map(e -> caseRepository.findFirstByEmployee_IdOrderByIdDesc(e.getId()))
                .flatMap(Optional::stream)
                .sorted(Comparator.comparing(OnboardingCase::getId).reversed())
                .map(this::toDto)
                .collect(Collectors.toList());
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
        c.setOffer(req.offerId() != null ? jobOfferRepository.getReferenceById(req.offerId()) : null);
        c.setAssignedHr(req.assignedHrUserId() != null ? userRepository.getReferenceById(req.assignedHrUserId()) : null);
        c.setNotes(req.notes());
        c = caseRepository.save(c);
        taskLifecycleService.addDefaultTasks(c);
        return toDto(caseRepository.findById(c.getId()).orElseThrow());
    }

    /**
     * When an offer is marked joined (no employee yet): creates or updates an onboarding case with default tasks.
     * The employee is created when the case is {@link #complete(Long) completed}.
     */
    @Transactional
    public void ensureOnboardingForJoinedOffer(long offerId, LocalDate joinDate, Long assignedHrUserId) {
        JobOffer offer = jobOfferRepository.findById(offerId)
                .orElseThrow(() -> new IllegalArgumentException("Offer not found: " + offerId));
        NameParts np = splitName(offer.getCandidateName());

        Optional<OnboardingCase> latestOpt = caseRepository.findFirstByOffer_IdOrderByIdDesc(offerId);
        if (latestOpt.isPresent()) {
            OnboardingCase existing = latestOpt.get();
            if (existing.getStatus() != OnboardingStatus.CANCELLED) {
                if (existing.getEmployee() != null) {
                    throw new IllegalStateException("Onboarding for this offer already has an employee linked");
                }
                existing.setJoinDate(joinDate);
                existing.setCandidateFirstName(np.firstName());
                existing.setCandidateLastName(np.lastName());
                if (offer.getCandidateEmail() != null && !offer.getCandidateEmail().isBlank()) {
                    existing.setCandidateEmail(offer.getCandidateEmail());
                }
                if (existing.getDepartment() == null && offer.getDepartment() != null) {
                    existing.setDepartment(offer.getDepartment());
                }
                if (existing.getDesignation() == null && offer.getDesignation() != null) {
                    existing.setDesignation(offer.getDesignation());
                }
                if (assignedHrUserId != null && existing.getAssignedHr() == null) {
                    existing.setAssignedHr(userRepository.getReferenceById(assignedHrUserId));
                }
                if (existing.getStatus() == OnboardingStatus.DRAFT || existing.getStatus() == OnboardingStatus.IN_PROGRESS) {
                    existing.setStatus(OnboardingStatus.IN_PROGRESS);
                }
                caseRepository.save(existing);
                if (taskLifecycleService.countTasksForCase(existing.getId()) == 0) {
                    taskLifecycleService.addDefaultTasks(existing);
                }
                return;
            }
        }

        OnboardingCase c = new OnboardingCase();
        c.setStatus(OnboardingStatus.IN_PROGRESS);
        c.setCandidateFirstName(np.firstName());
        c.setCandidateLastName(np.lastName());
        c.setCandidateEmail(offer.getCandidateEmail());
        c.setJoinDate(joinDate);
        c.setDepartment(offer.getDepartment());
        c.setDesignation(offer.getDesignation());
        c.setOffer(offer);
        if (assignedHrUserId != null) {
            c.setAssignedHr(userRepository.getReferenceById(assignedHrUserId));
        }
        c.setNotes("Created when the offer was marked joined.");
        c = caseRepository.save(c);
        taskLifecycleService.addDefaultTasks(c);
    }

    /**
     * When an employee moves to a separation status, ensures an onboarding case (creating one if needed)
     * has pending tasks to issue experience and relieving letters. Idempotent: skips task labels already present.
     */
    @Transactional
    public void ensureExitLetterTasks(Long employeeId) {
        requireHrAdmin();
        Employee emp = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + employeeId));
        OnboardingCase c = caseRepository.findFirstByEmployee_IdOrderByIdDesc(employeeId)
                .orElseGet(() -> createCaseForExitLetters(emp));
        enrichCaseNotesWithExitDetailsIfNeeded(c, emp);
        c = caseRepository.save(c);
        taskLifecycleService.addExitLetterTasksIfAbsent(c);
    }

    private OnboardingCase createCaseForExitLetters(Employee emp) {
        OnboardingCase c = new OnboardingCase();
        c.setStatus(OnboardingStatus.IN_PROGRESS);
        c.setEmployee(emp);
        c.setCandidateFirstName(emp.getFirstName());
        c.setCandidateLastName(emp.getLastName());
        c.setCandidateEmail(emp.getEmail());
        c.setJoinDate(emp.getJoinedAt() != null ? emp.getJoinedAt() : LocalDate.now());
        c.setDepartment(emp.getDepartment());
        c.setDesignation(emp.getDesignation());
        c.setNotes("Opened for experience and relieving letters (status: " + emp.getEmploymentStatus() + ").\n\n"
                + buildExitDetailFooter(emp));
        return caseRepository.save(c);
    }

    /** Appends last working date and reason to case notes when missing (e.g. hired employee, later given a separation status). */
    private void enrichCaseNotesWithExitDetailsIfNeeded(OnboardingCase c, Employee emp) {
        if (!EmploymentStatus.isSeparation(emp.getEmploymentStatus())) {
            return;
        }
        String footer = buildExitDetailFooter(emp);
        String n = c.getNotes() != null ? c.getNotes() : "";
        if (n.contains("Last working date:")) {
            return;
        }
        c.setNotes(n.isBlank() ? footer : n + "\n\n" + footer);
    }

    private static String buildExitDetailFooter(Employee emp) {
        return "Status: " + emp.getEmploymentStatus()
                + "\nLast working date: " + emp.getLastWorkingDate()
                + "\nReason: " + (emp.getExitReason() != null ? emp.getExitReason() : "");
    }

    @Transactional
    public OnboardingCaseDto addTask(Long caseId, OnboardingTaskCreateRequest req) {
        requireHrAdmin();
        User u = currentUserService.requireCurrentUser();
        taskLifecycleService.addTask(caseId, req, u);
        return toDto(caseRepository.findById(caseId).orElseThrow());
    }

    @Transactional
    public OnboardingCaseDto saveBankDetails(Long caseId, OnboardingBankDetailsUpsertRequest req) {
        requireHrAdmin();
        bankDetailsCommandService.saveBankDetails(caseId, req);
        return toDto(caseRepository.findById(caseId).orElseThrow());
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
        User u = currentUserService.requireCurrentUser();
        return taskLifecycleService.updateTask(caseId, taskId, req, u);
    }

    @Transactional
    public OnboardingCaseDto complete(Long id) {
        requireHrAdmin();
        OnboardingCase c = caseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Onboarding case not found: " + id));
        hireCompletionService.completeHire(c);
        return toDto(caseRepository.findById(id).orElseThrow());
    }

    private OnboardingCaseDto toDto(OnboardingCase c) {
        List<OnboardingTaskDto> tasks = taskLifecycleService.loadTasksWithAuditsForCase(c.getId());
        OnboardingBankDetailsDto bank = bankDetailsCommandService.findForCase(c.getId()).orElse(null);
        return OnboardingCaseDto.from(c, tasks, bank);
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

    private record NameParts(String firstName, String lastName) {}

    private static NameParts splitName(String full) {
        String s = full != null ? full.trim().replaceAll("\\s+", " ") : "";
        if (s.isBlank()) {
            return new NameParts("Employee", "—");
        }
        String[] parts = s.split(" ");
        if (parts.length == 1) {
            return new NameParts(parts[0], "—");
        }
        String first = parts[0];
        String last = String.join(" ", java.util.Arrays.copyOfRange(parts, 1, parts.length));
        return new NameParts(first, last);
    }
}
