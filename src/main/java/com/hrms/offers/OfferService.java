package com.hrms.offers;

import com.hrms.auth.entity.User;
import com.hrms.compensation.CompensationService;
import com.hrms.compensation.entity.EmployeeCompensation;
import com.hrms.compensation.entity.EmployeeCompensationLine;
import com.hrms.compensation.repository.EmployeeCompensationRepository;
import com.hrms.offers.dto.*;
import com.hrms.offers.entity.JobOffer;
import com.hrms.offers.entity.OfferCompensation;
import com.hrms.offers.entity.OfferCompensationLine;
import com.hrms.offers.entity.OfferTemplate;
import com.hrms.offers.repository.OfferCompensationRepository;
import com.hrms.offers.repository.JobOfferRepository;
import com.hrms.offers.repository.OfferTemplateRepository;
import com.hrms.leave.repository.LeaveBalanceRepository;
import com.hrms.leave.repository.LeaveTypeRepository;
import com.hrms.org.EmployeeRequest;
import com.hrms.org.EmployeeService;
import com.hrms.org.repository.DepartmentRepository;
import com.hrms.org.repository.DesignationRepository;
import com.hrms.org.repository.EmployeeRepository;
import com.hrms.payroll.entity.SalaryComponent;
import com.hrms.payroll.repository.SalaryComponentRepository;
import com.hrms.security.CurrentUserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OfferService {

    private final OfferTemplateRepository templateRepository;
    private final JobOfferRepository jobOfferRepository;
    private final DepartmentRepository departmentRepository;
    private final DesignationRepository designationRepository;
    private final EmployeeRepository employeeRepository;
    private final CurrentUserService currentUserService;
    private final OfferPdfService offerPdfService;
    private final OfferCompensationRepository offerCompensationRepository;
    private final SalaryComponentRepository salaryComponentRepository;
    private final OfferEmailService offerEmailService;
    private final EmployeeService employeeService;
    private final EmployeeCompensationRepository employeeCompensationRepository;
    private final CompensationService compensationService;
    private final TransactionTemplate transactionTemplate;
    private final LeaveTypeRepository leaveTypeRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;

    public OfferService(
            OfferTemplateRepository templateRepository,
            JobOfferRepository jobOfferRepository,
            DepartmentRepository departmentRepository,
            DesignationRepository designationRepository,
            EmployeeRepository employeeRepository,
            CurrentUserService currentUserService,
            OfferPdfService offerPdfService,
            OfferCompensationRepository offerCompensationRepository,
            SalaryComponentRepository salaryComponentRepository,
            OfferEmailService offerEmailService,
            EmployeeService employeeService,
            EmployeeCompensationRepository employeeCompensationRepository,
            CompensationService compensationService,
            TransactionTemplate transactionTemplate,
            LeaveTypeRepository leaveTypeRepository,
            LeaveBalanceRepository leaveBalanceRepository
    ) {
        this.templateRepository = templateRepository;
        this.jobOfferRepository = jobOfferRepository;
        this.departmentRepository = departmentRepository;
        this.designationRepository = designationRepository;
        this.employeeRepository = employeeRepository;
        this.currentUserService = currentUserService;
        this.offerPdfService = offerPdfService;
        this.offerCompensationRepository = offerCompensationRepository;
        this.salaryComponentRepository = salaryComponentRepository;
        this.offerEmailService = offerEmailService;
        this.employeeService = employeeService;
        this.employeeCompensationRepository = employeeCompensationRepository;
        this.compensationService = compensationService;
        this.transactionTemplate = transactionTemplate;
        this.leaveTypeRepository = leaveTypeRepository;
        this.leaveBalanceRepository = leaveBalanceRepository;
    }

    @Transactional(readOnly = true)
    public List<OfferTemplateDto> listTemplates() {
        requireHrAdmin();
        return templateRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(OfferTemplateDto::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OfferTemplateDto> listAllTemplatesAdmin() {
        requireHrAdmin();
        return templateRepository.findAll().stream()
                .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
                .map(OfferTemplateDto::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public OfferTemplateDto createTemplate(OfferTemplateRequest req) {
        requireHrAdmin();
        OfferTemplate t = new OfferTemplate();
        t.setName(req.name().trim());
        t.setBodyHtml(req.bodyHtml());
        t.setActive(req.active());
        return OfferTemplateDto.from(templateRepository.save(t));
    }

    @Transactional
    public OfferTemplateDto updateTemplate(Long id, OfferTemplateRequest req) {
        requireHrAdmin();
        OfferTemplate t = templateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Template not found: " + id));
        t.setName(req.name().trim());
        t.setBodyHtml(req.bodyHtml());
        t.setActive(req.active());
        return OfferTemplateDto.from(templateRepository.save(t));
    }

    @Transactional(readOnly = true)
    public List<JobOfferDto> listOffers() {
        requireHrAdmin();
        return jobOfferRepository.findAllByOrderByIdDesc().stream()
                .map(JobOfferDto::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<JobOfferDto> listOffersPaged(
            String status,
            String employeeType,
            String q,
            Long departmentId,
            Long designationId,
            Pageable pageable
    ) {
        requireHrAdmin();
        Specification<JobOffer> spec = OfferSpecifications.build(status, employeeType, q, departmentId, designationId);
        return jobOfferRepository.findAll(spec, pageable).map(JobOfferDto::from);
    }

    @Transactional(readOnly = true)
    public JobOfferDto getOffer(Long id) {
        requireHrAdmin();
        JobOffer o = jobOfferRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Offer not found: " + id));
        return JobOfferDto.from(o);
    }

    @Transactional
    public JobOfferDto createOffer(JobOfferCreateRequest req) {
        requireHrAdmin();
        JobOffer o = new JobOffer();
        if (req.templateId() != null) {
            o.setTemplate(templateRepository.findById(req.templateId())
                    .orElseThrow(() -> new IllegalArgumentException("Template not found: " + req.templateId())));
        }
        o.setCandidateName(req.candidateName().trim());
        o.setCandidateEmail(req.candidateEmail());
        o.setCandidateMobile(req.candidateMobile());
        o.setEmployeeType(req.employeeType());
        o.setDepartment(req.departmentId() != null ? departmentRepository.getReferenceById(req.departmentId()) : null);
        o.setDesignation(req.designationId() != null ? designationRepository.getReferenceById(req.designationId()) : null);
        o.setManager(req.managerId() != null ? employeeRepository.getReferenceById(req.managerId()) : null);
        o.setJoinDate(req.joinDate());
        o.setOfferReleaseDate(req.offerReleaseDate());
        o.setProbationPeriodMonths(req.probationPeriodMonths());
        o.setJoiningBonus(req.joiningBonus());
        o.setYearlyBonus(req.yearlyBonus());
        o.setAnnualCtc(req.annualCtc());
        o.setCurrency(req.currency() != null && !req.currency().isBlank() ? req.currency().trim() : "INR");
        o.setStatus(OfferStatus.DRAFT);
        o.setBodyHtml(mergeBody(o));
        JobOffer saved = jobOfferRepository.save(o);

        List<OfferCompensationLineRequest> lines = req.compensationLines() != null ? req.compensationLines() : List.of();
        if (!lines.isEmpty()) {
            OfferCompensation comp = new OfferCompensation();
            comp.setOffer(saved);
            comp.setCurrency(saved.getCurrency() != null ? saved.getCurrency() : "INR");
            comp.setAnnualCtc(saved.getAnnualCtc());
            for (OfferCompensationLineRequest lr : lines) {
                SalaryComponent sc = salaryComponentRepository.findById(lr.componentId())
                        .orElseThrow(() -> new IllegalArgumentException("Salary component not found: " + lr.componentId()));
                OfferCompensationLine line = new OfferCompensationLine();
                line.setCompensation(comp);
                line.setComponent(sc);
                line.setAmount(lr.amount());
                comp.getOfferCompensationLine().add(line);
            }
            offerCompensationRepository.save(comp);
        }

        return JobOfferDto.from(saved);
    }

    @Transactional
    public JobOfferDto refreshBody(Long id) {
        requireHrAdmin();
        JobOffer o = jobOfferRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Offer not found: " + id));
        o.setBodyHtml(mergeBody(o));
        return JobOfferDto.from(jobOfferRepository.save(o));
    }

    @Transactional
    public JobOfferDto releaseOffer(Long id, boolean forceResend) {
        requireHrAdmin();
        User u = currentUserService.requireCurrentUser();
        JobOffer o = jobOfferRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Offer not found: " + id));
        if (!forceResend && o.getStatus() != OfferStatus.DRAFT && o.getStatus() != OfferStatus.SENT) {
            throw new IllegalArgumentException("Cannot release offer in status: " + o.getStatus());
        }
        if (o.getCandidateEmail() == null || o.getCandidateEmail().isBlank()) {
            throw new IllegalArgumentException("Candidate personal email is required to release offer");
        }

        o.setBodyHtml(mergeBody(o));
        o.setStatus(OfferStatus.SENT);
        o.setSentAt(Instant.now());
        o.setSentByUserId(u.getId());
        if (o.getOfferReleaseDate() == null) {
            o.setOfferReleaseDate(LocalDate.now());
        }

        OfferPdfDownload d = generatePdfDownload(id);
        try {
            offerEmailService.sendOffer(o.getCandidateEmail(), o.getCandidateName(), d.bytes(), d.filename());
            o.setLastEmailStatus("SENT");
            o.setLastEmailError(null);
        } catch (Exception ex) {
            o.setLastEmailStatus("FAILED");
            o.setLastEmailError(truncate(ex.getMessage(), 2000));
        }

        return JobOfferDto.from(jobOfferRepository.save(o));
    }

    /**
     * Filename pattern: {@code {24hex}_{CandidateName}_Offer_Letter_{yyyyMMdd}_{HHmmss}.pdf}
     * (e.g. {@code 1642233994a61e2806cb4250_HariNale_Offer_Letter_20220115_133603.pdf})
     * <p>PDF bytes are built outside a DB transaction so long-running work does not hold connections.</p>
     */
    public OfferPdfDownload generatePdfDownload(Long id) {
        requireHrAdmin();
        JobOffer o = jobOfferRepository.findWithDepartmentAndDesignationById(id)
                .orElseThrow(() -> new IllegalArgumentException("Offer not found: " + id));
        if (o.getBodyHtml() == null || o.getBodyHtml().isBlank()) {
            o.setBodyHtml(mergeBody(o));
            jobOfferRepository.save(o);
        }
        OfferCompensation comp = offerCompensationRepository.findByOfferId(o.getId()).orElse(null);
        List<OfferPdfService.OfferCompLine> lines = List.of();
        if (comp != null && comp.getOfferCompensationLine() != null && !comp.getOfferCompensationLine().isEmpty()) {
            String currency = comp.getCurrency() != null ? comp.getCurrency() : (o.getCurrency() != null ? o.getCurrency() : "");
            lines = comp.getOfferCompensationLine().stream().map(l -> {
                String label = l.getComponent() != null
                        ? (l.getComponent().getCode() + " — " + l.getComponent().getName())
                        : "Component";
                String amt = formatMoney(l.getAmount(), currency);
                return new OfferPdfService.OfferCompLine(label, amt);
            }).toList();
        }
        OfferPdfService.OfferLetterPdfModel model = new OfferPdfService.OfferLetterPdfModel(
                o.getEmployeeType(),
                o.getCandidateName(),
                o.getCandidateEmail(),
                o.getCandidateMobile(),
                o.getJoinDate(),
                o.getOfferReleaseDate(),
                o.getProbationPeriodMonths() != null ? String.valueOf(o.getProbationPeriodMonths()) : "—",
                formatMoney(o.getJoiningBonus(), o.getCurrency()),
                formatMoney(o.getYearlyBonus(), o.getCurrency()),
                o.getDesignation() != null ? o.getDesignation().getName() : "—",
                o.getDepartment() != null ? o.getDepartment().getName() : "—",
                formatMoney(o.getAnnualCtc(), o.getCurrency()),
                lines
        );
        byte[] pdf = offerPdfService.generateOfferLetter(model);
        transactionTemplate.executeWithoutResult(status -> {
//            o = jobOfferRepository.findById(id)
//                    .orElseThrow(() -> new IllegalArgumentException("Offer not found: " + id));
//            o.setPdfGeneratedAt(Instant.now()); TODO:: check why we need this code.
        });
        return new OfferPdfDownload(pdf, buildOfferPdfFilename(model.employeeName()));
    }

    public static String buildOfferPdfFilename(String candidateName) {
        String hex = UUID.randomUUID().toString().replace("-", "").substring(0, 24);
        String raw = candidateName != null ? candidateName : "Candidate";
        String safe = raw.replaceAll("[^A-Za-z0-9]", "");
        if (safe.isEmpty()) {
            safe = "Candidate";
        }
        if (safe.length() > 48) {
            safe = safe.substring(0, 48);
        }
        String stamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(LocalDateTime.now());
        return hex + "_" + safe + "_Offer_Letter_" + stamp + ".pdf";
    }

    public record OfferPdfDownload(byte[] bytes, String filename) {}

    @Transactional
    public JobOfferDto acceptOffer(Long id) {
        requireHrAdmin();
        User u = currentUserService.requireCurrentUser();
        JobOffer o = jobOfferRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Offer not found: " + id));
        if (o.getStatus() == OfferStatus.JOINED) {
            throw new IllegalArgumentException("Offer already joined");
        }
        o.setStatus(OfferStatus.ACCEPTED);
        o.setAcceptedAt(Instant.now());
        o.setAcceptedByUserId(u.getId());
        return JobOfferDto.from(jobOfferRepository.save(o));
    }

    @Transactional
    public JobOfferDto rejectOffer(Long id) {
        requireHrAdmin();
        User u = currentUserService.requireCurrentUser();
        JobOffer o = jobOfferRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Offer not found: " + id));
        if (o.getStatus() == OfferStatus.JOINED) {
            throw new IllegalArgumentException("Offer already joined");
        }
        o.setStatus(OfferStatus.REJECTED);
        o.setRejectedAt(Instant.now());
        o.setRejectedByUserId(u.getId());
        return JobOfferDto.from(jobOfferRepository.save(o));
    }

    @Transactional
    public JobOfferDto markJoined(Long id, @Valid MarkJoinedRequest body) {
        requireHrAdmin();
        User u = currentUserService.requireCurrentUser();
        JobOffer o = jobOfferRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Offer not found: " + id));
        if (o.getStatus() != OfferStatus.ACCEPTED) {
            throw new IllegalArgumentException("Only accepted offers can be marked joined");
        }
        if (o.getEmployee() != null) {
            throw new IllegalArgumentException("Employee already created for this offer");
        }
        if (o.getCandidateEmail() == null || o.getCandidateEmail().isBlank()) {
            throw new IllegalArgumentException("Candidate email is required to create employee");
        }
        if (o.getCandidateMobile() == null || o.getCandidateMobile().isBlank()) {
            throw new IllegalArgumentException("Candidate mobile number is required to create employee");
        }

        NameParts np = splitName(o.getCandidateName());
        EmployeeRequest empReq = new EmployeeRequest(
                null,
                np.firstName(),
                np.lastName(),
                o.getCandidateEmail(),
                o.getCandidateMobile(),
                o.getDepartment() != null ? o.getDepartment().getId() : null,
                o.getDesignation() != null ? o.getDesignation().getId() : null,
                o.getManager() != null ? o.getManager().getId() : null,
                o.getJoinDate()
        );
        var createdEmployee = employeeService.create(empReq);

        o.setEmployee(employeeRepository.getReferenceById(createdEmployee.id()));
        o.setStatus(OfferStatus.JOINED);
        o.setJoinedAt(Instant.now());
        o.setJoinedByUserId(u.getId());

        OfferCompensation offerComp = offerCompensationRepository.findByOfferId(o.getId())
                .orElseThrow(() -> new IllegalArgumentException("Offer compensation is required to create employee compensation"));
        if (offerComp.getOfferCompensationLine() == null || offerComp.getOfferCompensationLine().isEmpty()) {
            throw new IllegalArgumentException("Offer compensation lines are required");
        }

        EmployeeCompensation c = new EmployeeCompensation();
        c.setEmployee(employeeRepository.getReferenceById(createdEmployee.id()));
        c.setEffectiveFrom(o.getJoinDate() != null ? o.getJoinDate() : LocalDate.now());
        c.setCurrency(offerComp.getCurrency() != null ? offerComp.getCurrency() : (o.getCurrency() != null ? o.getCurrency() : "INR"));
        c.setAnnualCtc(offerComp.getAnnualCtc() != null ? offerComp.getAnnualCtc() : o.getAnnualCtc());
        c.setNotes("Created from offer #" + o.getId());

        for (OfferCompensationLine ol : offerComp.getOfferCompensationLine()) {
            EmployeeCompensationLine nl = new EmployeeCompensationLine();
            nl.setCompensation(c);
            nl.setComponent(ol.getComponent());
            nl.setAmount(ol.getAmount() != null ? ol.getAmount() : BigDecimal.ZERO);
            c.getLines().add(nl);
        }

        EmployeeCompensation saved = employeeCompensationRepository.save(c);
        compensationService.syncToSalaryStructure(saved.getId());

        return JobOfferDto.from(jobOfferRepository.save(o));
    }

    public record CsvExport(String filename, String csv) {}

    @Transactional(readOnly = true)
    public CsvExport exportOffersCsv(String status, String employeeType, String q, Long departmentId, Long designationId) {
        requireHrAdmin();
        Specification<JobOffer> spec = OfferSpecifications.build(status, employeeType, q, departmentId, designationId);
        List<JobOfferDto> rows = jobOfferRepository.findAll(spec).stream().map(JobOfferDto::from).toList();
        String csv = OfferCsvExporter.toCsv(rows);
        String stamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(LocalDateTime.now());
        return new CsvExport("offers_" + stamp + ".csv", csv);
    }

    private String mergeBody(JobOffer o) {
        String raw = o.getTemplate() != null ? o.getTemplate().getBodyHtml() : "";
        if (raw.isBlank()) {
            raw = "<p>Dear {{candidateName}},</p><p>We are pleased to offer you a position. Start date: {{joinDate}}. Annual CTC: {{currency}} {{annualCtc}}.</p>";
        }
        String dept = o.getDepartment() != null ? o.getDepartment().getName() : "";
        String des = o.getDesignation() != null ? o.getDesignation().getName() : "";
        String mgr = o.getManager() != null
                ? (o.getManager().getFirstName() + " " + o.getManager().getLastName()).trim()
                : "";
        String join = o.getJoinDate() != null ? o.getJoinDate().toString() : "";
        String ctc = o.getAnnualCtc() != null ? o.getAnnualCtc().toPlainString() : "";
        String release = o.getOfferReleaseDate() != null ? o.getOfferReleaseDate().toString() : "";
        String probation = o.getProbationPeriodMonths() != null ? String.valueOf(o.getProbationPeriodMonths()) : "";
        String joinBonus = o.getJoiningBonus() != null ? o.getJoiningBonus().toPlainString() : "";
        String yearlyBonus = o.getYearlyBonus() != null ? o.getYearlyBonus().toPlainString() : "";
        String empType = o.getEmployeeType() != null ? o.getEmployeeType() : "";
        return raw
                .replace("{{candidateName}}", o.getCandidateName() != null ? o.getCandidateName() : "")
                .replace("{{department}}", dept)
                .replace("{{designation}}", des)
                .replace("{{managerName}}", mgr)
                .replace("{{joinDate}}", join)
                .replace("{{annualCtc}}", ctc)
                .replace("{{currency}}", o.getCurrency() != null ? o.getCurrency() : "")
                .replace("{{offerReleaseDate}}", release)
                .replace("{{probationMonths}}", probation)
                .replace("{{joiningBonus}}", joinBonus)
                .replace("{{yearlyBonus}}", yearlyBonus)
                .replace("{{employeeType}}", empType);
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

    private static String truncate(String s, int max) {
        if (s == null) return null;
        if (s.length() <= max) return s;
        return s.substring(0, Math.max(0, max - 3)) + "...";
    }

    private static String formatMoney(BigDecimal amt, String currency) {
        if (amt == null) return "—";
        String cur = currency != null && !currency.isBlank() ? currency : "";
        return (cur.isBlank() ? "" : cur + " ") + amt.toPlainString();
    }

    private record NameParts(String firstName, String lastName) {}

    private static NameParts splitName(String full) {
        String s = full != null ? full.trim().replaceAll("\\s+", " ") : "";
        if (s.isBlank()) return new NameParts("Employee", "—");
        String[] parts = s.split(" ");
        if (parts.length == 1) return new NameParts(parts[0], "—");
        String first = parts[0];
        String last = String.join(" ", java.util.Arrays.copyOfRange(parts, 1, parts.length));
        return new NameParts(first, last);
    }
}
