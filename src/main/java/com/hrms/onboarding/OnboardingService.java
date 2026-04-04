package com.hrms.onboarding;

import com.hrms.auth.EmployeeAccountService;
import com.hrms.auth.entity.User;
import com.hrms.auth.repository.UserRepository;
import com.hrms.compensation.CompensationFrequency;
import com.hrms.compensation.entity.EmployeeCompensation;
import com.hrms.compensation.entity.EmployeeCompensationLine;
import com.hrms.compensation.repository.EmployeeCompensationRepository;
import com.hrms.leave.entity.LeaveBalance;
import com.hrms.leave.entity.LeaveType;
import com.hrms.leave.repository.LeaveBalanceRepository;
import com.hrms.leave.repository.LeaveTypeRepository;
import com.hrms.offers.entity.JobOffer;
import com.hrms.offers.entity.OfferCompensation;
import com.hrms.offers.entity.OfferCompensationLine;
import com.hrms.offers.repository.JobOfferRepository;
import com.hrms.offers.repository.OfferCompensationRepository;
import com.hrms.onboarding.dto.*;
import com.hrms.onboarding.entity.OnboardingBankDetails;
import com.hrms.onboarding.entity.OnboardingCase;
import com.hrms.onboarding.entity.OnboardingTask;
import com.hrms.onboarding.entity.OnboardingTaskAudit;
import com.hrms.onboarding.repository.OnboardingBankDetailsRepository;
import com.hrms.onboarding.repository.OnboardingCaseRepository;
import com.hrms.onboarding.repository.OnboardingTaskAuditRepository;
import com.hrms.onboarding.repository.OnboardingTaskRepository;
import com.hrms.payroll.EmployeePayrollBankService;
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
import java.time.LocalDate;
import java.time.Year;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
    private final EmployeeAccountService employeeAccountService;
    private final OfferCompensationRepository offerCompensationRepository;
    private final EmployeeCompensationRepository employeeCompensationRepository;
    private final OnboardingTaskAuditRepository taskAuditRepository;
    private final OnboardingBankDetailsRepository bankDetailsRepository;
    private final EmployeePayrollBankService employeePayrollBankService;

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
            CurrentUserService currentUserService,
            EmployeeAccountService employeeAccountService,
            OfferCompensationRepository offerCompensationRepository,
            EmployeeCompensationRepository employeeCompensationRepository,
            OnboardingTaskAuditRepository taskAuditRepository,
            OnboardingBankDetailsRepository bankDetailsRepository,
            EmployeePayrollBankService employeePayrollBankService
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
        this.employeeAccountService = employeeAccountService;
        this.offerCompensationRepository = offerCompensationRepository;
        this.employeeCompensationRepository = employeeCompensationRepository;
        this.taskAuditRepository = taskAuditRepository;
        this.bankDetailsRepository = bankDetailsRepository;
        this.employeePayrollBankService = employeePayrollBankService;
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
        addDefaultTasks(c);
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
                if (taskRepository.countByOnboardingCaseId(existing.getId()) == 0) {
                    addDefaultTasks(existing);
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
        addDefaultTasks(c);
    }

    private void addDefaultTasks(OnboardingCase c) {
        int order = 0;
        for (String label : DEFAULT_TASKS) {
            OnboardingTask t = new OnboardingTask();
            t.setOnboardingCase(c);
            t.setLabel(label);
            t.setStatus(OnboardingTaskStatus.PENDING);
            t.setDone(false);
            t.setSortOrder(order++);
            taskRepository.save(t);
        }
    }

    @Transactional
    public OnboardingCaseDto addTask(Long caseId, OnboardingTaskCreateRequest req) {
        requireHrAdmin();
        User u = currentUserService.requireCurrentUser();
        OnboardingCase c = caseRepository.findById(caseId)
                .orElseThrow(() -> new IllegalArgumentException("Onboarding case not found: " + caseId));
        ensureCaseEditable(c);
        int max = taskRepository.findMaxSortOrderForCase(caseId);
        OnboardingTask t = new OnboardingTask();
        t.setOnboardingCase(c);
        t.setLabel(req.name().trim());
        t.setStatus(OnboardingTaskStatus.PENDING);
        t.setDone(false);
        t.setSortOrder(max + 1);
        t = taskRepository.save(t);
        recordAudit(t, "CREATED", "Task created: " + t.getLabel(), u);
        return toDto(caseRepository.findById(caseId).orElseThrow());
    }

    @Transactional
    public OnboardingCaseDto saveBankDetails(Long caseId, OnboardingBankDetailsUpsertRequest req) {
        requireHrAdmin();
        OnboardingCase c = caseRepository.findById(caseId)
                .orElseThrow(() -> new IllegalArgumentException("Onboarding case not found: " + caseId));
        ensureBankDetailsEditable(c);
        OnboardingBankAccountType accountType;
        try {
            accountType = OnboardingBankAccountType.valueOf(req.accountType().trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("accountType must be SAVINGS or CURRENT");
        }
        OnboardingBankDetails b = bankDetailsRepository.findByOnboardingCase_Id(caseId).orElseGet(() -> {
            OnboardingBankDetails nb = new OnboardingBankDetails();
            nb.setOnboardingCase(c);
            return nb;
        });
        b.setAccountHolderName(req.accountHolderName().trim());
        b.setBankName(req.bankName().trim());
        b.setBranch(req.branch() != null && !req.branch().isBlank() ? req.branch().trim() : null);
        b.setAccountNumber(req.accountNumber().trim());
        b.setIfscCode(req.ifscCode().trim().toUpperCase());
        b.setAccountType(accountType);
        b.setNotes(req.notes() != null && !req.notes().isBlank() ? req.notes().trim() : null);
        bankDetailsRepository.save(b);
        User u = currentUserService.requireCurrentUser();
        LocalDate eff = req.effectiveFrom() != null ? req.effectiveFrom() : LocalDate.now();
        if (c.getEmployee() != null) {
            employeePayrollBankService.syncFromOnboardingCaseBank(c, b, eff, u);
        }
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
        if (req == null || (req.done() == null && req.status() == null && req.comment() == null && req.name() == null)) {
            throw new IllegalArgumentException("No changes provided");
        }
        OnboardingTask t = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));
        if (!t.getOnboardingCase().getId().equals(caseId)) {
            throw new IllegalArgumentException("Task does not belong to case");
        }
        ensureCaseEditable(t.getOnboardingCase());

        OnboardingTaskStatus oldStatus = t.getStatus();
        String oldLabel = t.getLabel();
        String oldComment = t.getCommentText();

        if (req.name() != null) {
            String nm = req.name().trim();
            if (!nm.isEmpty() && !nm.equals(oldLabel)) {
                t.setLabel(nm);
                recordAudit(t, "NAME_CHANGED", "Name: '" + oldLabel + "' -> '" + nm + "'", u);
            }
        }

        if (req.status() != null && !req.status().isBlank()) {
            OnboardingTaskStatus s;
            try {
                s = OnboardingTaskStatus.valueOf(req.status().trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("Invalid task status: " + req.status());
            }
            if (s != oldStatus) {
                applyStatus(t, s);
                recordAudit(t, "STATUS_CHANGED", "Status: " + oldStatus + " -> " + s, u);
            }
        } else if (req.done() != null) {
            if (Boolean.TRUE.equals(req.done())) {
                if (oldStatus != OnboardingTaskStatus.DONE) {
                    applyStatus(t, OnboardingTaskStatus.DONE);
                    recordAudit(t, "STATUS_CHANGED", "Status: " + oldStatus + " -> DONE (done flag)", u);
                }
            } else if (oldStatus == OnboardingTaskStatus.DONE) {
                applyStatus(t, OnboardingTaskStatus.PENDING);
                recordAudit(t, "STATUS_CHANGED", "Status: DONE -> PENDING (done flag)", u);
            } else {
                t.setDone(false);
            }
        }

        if (req.comment() != null) {
            String newComment = req.comment().isBlank() ? null : req.comment();
            if (!Objects.equals(newComment, oldComment)) {
                t.setCommentText(newComment);
                recordAudit(t, "COMMENT_UPDATED",
                        "Comment " + (newComment == null ? "cleared" : "updated"), u);
            }
        }

        t = taskRepository.save(t);
        List<OnboardingTaskAuditDto> audits = loadAuditsByTaskId(List.of(t.getId())).getOrDefault(t.getId(), List.of());
        return OnboardingTaskDto.from(t, audits);
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
        JobOffer linkedOffer = null;
        if (c.getOffer() != null) {
            linkedOffer = jobOfferRepository.findById(c.getOffer().getId()).orElse(null);
            if (linkedOffer != null) {
                if (linkedOffer.getCandidateEmail() != null && !linkedOffer.getCandidateEmail().isBlank()) {
                    e.setEmail(linkedOffer.getCandidateEmail());
                }
                if (linkedOffer.getCandidateMobile() != null && !linkedOffer.getCandidateMobile().isBlank()) {
                    e.setMobileNumber(linkedOffer.getCandidateMobile());
                }
            }
        }
        final Employee saved = employeeRepository.save(e);
        employeeAccountService.provisionUserForNewEmployee(saved);

        c.setEmployee(saved);
        c.setStatus(OnboardingStatus.COMPLETED);
        caseRepository.save(c);

        employeePayrollBankService.ensureFromOnboardingAfterHire(c.getId(), saved.getId(),
                currentUserService.requireCurrentUser());

        if (linkedOffer != null) {
            linkedOffer.setEmployee(saved);
            jobOfferRepository.save(linkedOffer);
            copyOfferCompensationToEmployee(linkedOffer, saved, c.getJoinDate());
        }

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

    /**
     * Copies {@link OfferCompensation} lines to a new {@link EmployeeCompensation} when onboarding completes
     * for an offer-linked case. No-op if the offer has no compensation or no lines.
     */
    private void copyOfferCompensationToEmployee(JobOffer offer, Employee employee, LocalDate effectiveFrom) {
        OfferCompensation offerComp = offerCompensationRepository.findByOfferId(offer.getId()).orElse(null);
        if (offerComp == null || offerComp.getOfferCompensationLine() == null
                || offerComp.getOfferCompensationLine().isEmpty()) {
            return;
        }

        EmployeeCompensation comp = new EmployeeCompensation();
        comp.setEmployee(employee);
        comp.setEffectiveFrom(effectiveFrom);
        comp.setCurrency(offerComp.getCurrency() != null ? offerComp.getCurrency()
                : (offer.getCurrency() != null ? offer.getCurrency() : "INR"));
        comp.setNotes("Created from offer #" + offer.getId());

        for (OfferCompensationLine ol : offerComp.getOfferCompensationLine()) {
            EmployeeCompensationLine nl = new EmployeeCompensationLine();
            nl.setCompensation(comp);
            nl.setComponent(ol.getComponent());
            nl.setAmount(ol.getAmount() != null ? ol.getAmount() : BigDecimal.ZERO);
            CompensationFrequency freq = ol.getFrequency() != null ? ol.getFrequency() : CompensationFrequency.MONTHLY;
            nl.setFrequency(freq);
            if (freq == CompensationFrequency.ONE_TIME) {
                nl.setPayableOn(effectiveFrom);
            }
            comp.getLines().add(nl);
        }
        comp.setAnnualCtc(comp.calculateAnnualCtc());
        employeeCompensationRepository.save(comp);
    }

    private OnboardingCaseDto toDto(OnboardingCase c) {
        List<OnboardingTask> taskEntities = taskRepository.findByOnboardingCaseIdOrderBySortOrderAsc(c.getId());
        List<Long> taskIds = taskEntities.stream().map(OnboardingTask::getId).toList();
        Map<Long, List<OnboardingTaskAuditDto>> auditsByTask = loadAuditsByTaskId(taskIds);
        List<OnboardingTaskDto> tasks = taskEntities.stream()
                .map(t -> OnboardingTaskDto.from(t, auditsByTask.getOrDefault(t.getId(), List.of())))
                .toList();
        OnboardingBankDetailsDto bank = bankDetailsRepository.findByOnboardingCase_Id(c.getId())
                .map(OnboardingBankDetailsDto::from)
                .orElse(null);
        return OnboardingCaseDto.from(c, tasks, bank);
    }

    private void ensureCaseEditable(OnboardingCase c) {
        if (c.getStatus() == OnboardingStatus.COMPLETED) {
            throw new IllegalArgumentException("Cannot modify a completed onboarding case");
        }
    }

    /** Bank details may be updated after hire (e.g. employee bank change requests). */
    private void ensureBankDetailsEditable(OnboardingCase c) {
        if (c.getStatus() == OnboardingStatus.CANCELLED) {
            throw new IllegalArgumentException("Cannot modify bank details for a cancelled onboarding case");
        }
    }

    private static void applyStatus(OnboardingTask t, OnboardingTaskStatus s) {
        t.setStatus(s);
        t.setDone(s == OnboardingTaskStatus.DONE);
    }

    private void recordAudit(OnboardingTask task, String action, String detail, User u) {
        OnboardingTaskAudit a = new OnboardingTaskAudit();
        a.setTask(task);
        a.setAction(action);
        a.setDetail(detail);
        if (u != null) {
            a.setCreatedByUserId(u.getId());
            a.setCreatedByUsername(u.getUsername());
        }
        taskAuditRepository.save(a);
    }

    private Map<Long, List<OnboardingTaskAuditDto>> loadAuditsByTaskId(List<Long> taskIds) {
        if (taskIds.isEmpty()) {
            return Map.of();
        }
        List<OnboardingTaskAudit> rows = taskAuditRepository.findByTask_IdIn(taskIds);
        rows.sort(Comparator.comparing(OnboardingTaskAudit::getCreatedAt).reversed());
        Map<Long, List<OnboardingTaskAuditDto>> map = new LinkedHashMap<>();
        for (OnboardingTaskAudit row : rows) {
            Long tid = row.getTask().getId();
            map.computeIfAbsent(tid, k -> new ArrayList<>()).add(OnboardingTaskAuditDto.from(row));
        }
        return map;
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
