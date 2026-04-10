package com.hrms.onboarding;

import com.hrms.auth.entity.User;
import com.hrms.onboarding.dto.OnboardingTaskAuditDto;
import com.hrms.onboarding.dto.OnboardingTaskCreateRequest;
import com.hrms.onboarding.dto.OnboardingTaskDto;
import com.hrms.onboarding.dto.OnboardingTaskUpdateRequest;
import com.hrms.onboarding.entity.OnboardingCase;
import com.hrms.onboarding.entity.OnboardingTask;
import com.hrms.onboarding.entity.OnboardingTaskAudit;
import com.hrms.onboarding.repository.OnboardingCaseRepository;
import com.hrms.onboarding.repository.OnboardingTaskAuditRepository;
import com.hrms.onboarding.repository.OnboardingTaskRepository;
import com.hrms.org.entity.Employee;
import com.hrms.security.CurrentUserService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Default and custom onboarding tasks, exit-letter tasks, audits, and task updates.
 */
@Service
public class OnboardingTaskLifecycleService {

    private static final List<String> DEFAULT_TASKS = List.of(
            "Collect signed documents",
            "IT access & equipment",
            "Payroll & bank details",
            "Orientation schedule"
    );

    private static final List<String> EXIT_LETTER_TASKS = List.of(
            "Issue relieving letter",
            "Issue experience letter",
            "Issue full and final letter"
    );

    private final OnboardingTaskRepository taskRepository;
    private final OnboardingTaskAuditRepository taskAuditRepository;
    private final OnboardingCaseRepository caseRepository;
    private final CurrentUserService currentUserService;

    public OnboardingTaskLifecycleService(
            OnboardingTaskRepository taskRepository,
            OnboardingTaskAuditRepository taskAuditRepository,
            OnboardingCaseRepository caseRepository,
            CurrentUserService currentUserService
    ) {
        this.taskRepository = taskRepository;
        this.taskAuditRepository = taskAuditRepository;
        this.caseRepository = caseRepository;
        this.currentUserService = currentUserService;
    }

    public void addDefaultTasks(OnboardingCase c) {
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

    public void addExitLetterTasksIfAbsent(OnboardingCase c) {
        if (c.getStatus() == OnboardingStatus.CANCELLED) {
            throw new IllegalArgumentException("Cannot add letter tasks to a cancelled onboarding case");
        }
        List<OnboardingTask> existing = taskRepository.findByOnboardingCaseIdOrderBySortOrderAsc(c.getId());
        User u = currentUserService.requireCurrentUser();
        int order = taskRepository.findMaxSortOrderForCase(c.getId()) + 1;
        for (String label : EXIT_LETTER_TASKS) {
            boolean has = existing.stream()
                    .anyMatch(t -> label.equalsIgnoreCase(t.getLabel().trim()));
            if (!has) {
                OnboardingTask t = new OnboardingTask();
                t.setOnboardingCase(c);
                t.setLabel(label);
                t.setStatus(OnboardingTaskStatus.PENDING);
                t.setDone(false);
                t.setSortOrder(order++);
                t = taskRepository.save(t);
                recordAudit(t, "CREATED", "Task created: " + t.getLabel(), u);
            }
        }
    }

    public void addTask(Long caseId, OnboardingTaskCreateRequest req, User actor) {
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
        recordAudit(t, "CREATED", "Task created: " + t.getLabel(), actor);
    }

    public OnboardingTaskDto updateTask(Long caseId, Long taskId, OnboardingTaskUpdateRequest req, User actor) {
        if (req == null || (req.done() == null && req.status() == null && req.comment() == null && req.name() == null)) {
            throw new IllegalArgumentException("No changes provided");
        }
        OnboardingTask t = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));
        if (!t.getOnboardingCase().getId().equals(caseId)) {
            throw new IllegalArgumentException("Task does not belong to case");
        }
        OnboardingCase oc = t.getOnboardingCase();
        ensureCaseEditable(oc);

        OnboardingTaskStatus oldStatus = t.getStatus();
        String oldLabel = t.getLabel();
        String oldComment = t.getCommentText();

        if (req.name() != null) {
            String nm = req.name().trim();
            if (!nm.isEmpty() && !nm.equals(oldLabel)) {
                t.setLabel(nm);
                recordAudit(t, "NAME_CHANGED", "Name: '" + oldLabel + "' -> '" + nm + "'", actor);
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
                if (s == OnboardingTaskStatus.DONE) {
                    assertExitDocumentCompletionAllowed(t, oc);
                }
                applyStatus(t, s);
                recordAudit(t, "STATUS_CHANGED", "Status: " + oldStatus + " -> " + s, actor);
            }
        } else if (req.done() != null) {
            if (Boolean.TRUE.equals(req.done())) {
                if (oldStatus != OnboardingTaskStatus.DONE) {
                    assertExitDocumentCompletionAllowed(t, oc);
                    applyStatus(t, OnboardingTaskStatus.DONE);
                    recordAudit(t, "STATUS_CHANGED", "Status: " + oldStatus + " -> DONE (done flag)", actor);
                }
            } else if (oldStatus == OnboardingTaskStatus.DONE) {
                applyStatus(t, OnboardingTaskStatus.PENDING);
                recordAudit(t, "STATUS_CHANGED", "Status: DONE -> PENDING (done flag)", actor);
            } else {
                t.setDone(false);
            }
        }

        if (req.comment() != null) {
            String newComment = req.comment().isBlank() ? null : req.comment();
            if (!Objects.equals(newComment, oldComment)) {
                t.setCommentText(newComment);
                recordAudit(t, "COMMENT_UPDATED",
                        "Comment " + (newComment == null ? "cleared" : "updated"), actor);
            }
        }

        t = taskRepository.save(t);
        List<OnboardingTaskAuditDto> audits = loadAuditsByTaskId(List.of(t.getId())).getOrDefault(t.getId(), List.of());
        return OnboardingTaskDto.from(t, audits);
    }

    public List<OnboardingTaskDto> loadTasksWithAuditsForCase(long caseId) {
        List<OnboardingTask> taskEntities = taskRepository.findByOnboardingCaseIdOrderBySortOrderAsc(caseId);
        List<Long> taskIds = taskEntities.stream().map(OnboardingTask::getId).toList();
        Map<Long, List<OnboardingTaskAuditDto>> auditsByTask = loadAuditsByTaskId(taskIds);
        return taskEntities.stream()
                .map(t -> OnboardingTaskDto.from(t, auditsByTask.getOrDefault(t.getId(), List.of())))
                .toList();
    }

    public long countTasksForCase(long caseId) {
        return taskRepository.countByOnboardingCaseId(caseId);
    }

    private void ensureCaseEditable(OnboardingCase c) {
        if (c.getStatus() == OnboardingStatus.CANCELLED) {
            throw new IllegalArgumentException("Cannot modify tasks for a cancelled onboarding case");
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

    private static boolean isExitDocumentTaskLabel(String label) {
        if (label == null || label.isBlank()) {
            return false;
        }
        String n = label.trim();
        for (String canonical : EXIT_LETTER_TASKS) {
            if (canonical.equalsIgnoreCase(n)) {
                return true;
            }
        }
        return false;
    }

    private void assertExitDocumentCompletionAllowed(OnboardingTask t, OnboardingCase c) {
        if (!isExitDocumentTaskLabel(t.getLabel())) {
            return;
        }
        Employee emp = c.getEmployee();
        if (emp == null) {
            return;
        }
        LocalDate lwd = emp.getLastWorkingDate();
        LocalDate today = LocalDate.now();
        if (lwd == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Set the employee last working date before completing exit document tasks "
                            + "(relieving / experience / full & final).");
        }
        if (today.isBefore(lwd)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Exit document tasks can only be completed on or after the employee last working date ("
                            + lwd + "). Today is " + today + ". "
                            + "If letters are ready earlier, set or backdate last working date on the employee first.");
        }
    }
}
