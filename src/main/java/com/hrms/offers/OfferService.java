package com.hrms.offers;

import com.hrms.auth.entity.User;
import com.hrms.compensation.CompensationService;
import com.hrms.compensation.entity.EmployeeCompensation;
import com.hrms.compensation.entity.EmployeeCompensationLine;
import com.hrms.compensation.repository.EmployeeCompensationRepository;
import com.hrms.offers.dto.*;
import com.hrms.offers.entity.JobOffer;
import com.hrms.offers.entity.JobOfferEvent;
import com.hrms.offers.entity.OfferCompensation;
import com.hrms.offers.entity.OfferCompensationLine;
import com.hrms.offers.repository.JobOfferEventRepository;
import com.hrms.offers.repository.OfferCompensationRepository;
import com.hrms.offers.repository.JobOfferRepository;
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
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OfferService {

    private final JobOfferRepository jobOfferRepository;
    private final JobOfferEventRepository jobOfferEventRepository;
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

    public OfferService(
            OfferCompensationRepository offerCompensationRepository,
            JobOfferRepository jobOfferRepository,
            JobOfferEventRepository jobOfferEventRepository,
            DepartmentRepository departmentRepository,
            DesignationRepository designationRepository,
            EmployeeRepository employeeRepository,
            CurrentUserService currentUserService,
            OfferPdfService offerPdfService,
            SalaryComponentRepository salaryComponentRepository,
            OfferEmailService offerEmailService,
            EmployeeService employeeService,
            EmployeeCompensationRepository employeeCompensationRepository,
            CompensationService compensationService
    ) {
        this.jobOfferRepository = jobOfferRepository;
        this.jobOfferEventRepository = jobOfferEventRepository;
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
    }

    @Transactional(readOnly = true)
    public List<OfferDto> listOffers() {
        requireHrAdmin();
        return jobOfferRepository.findAllByOrderByIdDesc().stream()
                .map(OfferDto::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<OfferDto> listOffersPaged(
            String status,
            String employeeType,
            String searchQuery,
            Long departmentId,
            Long designationId,
            Pageable pageable
    ) {
        requireHrAdmin();
        Specification<JobOffer> spec = OfferSpecifications.build(status, employeeType, searchQuery, departmentId, designationId);
        return jobOfferRepository.findAll(spec, pageable).map(OfferDto::from);
    }

    @Transactional(readOnly = true)
    public OfferDto getOffer(Long id) {
        requireHrAdmin();
        JobOffer o = jobOfferRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Offer not found: " + id));
        return OfferDto.from(o);
    }

    @Transactional
    public OfferDto createOffer(OfferCreateRequest req) {
        requireHrAdmin();
        JobOffer o = new JobOffer();
        o.setCandidateName(req.candidateName().trim());
        o.setCandidateEmail(req.candidateEmail());
        o.setCandidateMobile(req.candidateMobile());
        o.setEmployeeType(EmployeeType.fromStringOrThrow(req.employeeType()));
        o.setDepartment(req.departmentId() != null ? departmentRepository.getReferenceById(req.departmentId()) : null);
        o.setDesignation(req.designationId() != null ? designationRepository.getReferenceById(req.designationId()) : null);
        o.setJoiningDate(req.joiningDate());
        o.setProbationPeriodMonths(req.probationPeriodMonths());
        o.setStatus(OfferStatus.DRAFT);
        JobOffer saved = jobOfferRepository.save(o);

        List<OfferCompensationLineRequest> lines = req.compensationLines() != null ? req.compensationLines() : List.of();
        BigDecimal annualCtc = req.annualCtc();
        if (annualCtc == null && !lines.isEmpty()) {
            annualCtc = calculateAnnualCtc(lines);
        }

        if (annualCtc != null) {
            saved.setAnnualCtc(annualCtc.setScale(2, RoundingMode.HALF_UP));
            saved = jobOfferRepository.save(saved);
        }

        if (!lines.isEmpty()) {
            OfferCompensation comp = new OfferCompensation();
            comp.setAnnualCtc(saved.getAnnualCtc() != null ? saved.getAnnualCtc() : BigDecimal.ZERO);
            comp.setOffer(saved);
            comp.setCurrency(saved.getCurrency() != null ? saved.getCurrency() : "INR");
            for (OfferCompensationLineRequest requestLine : lines) {
                SalaryComponent sc = salaryComponentRepository.findById(requestLine.componentId())
                        .orElseThrow(() -> new IllegalArgumentException("Salary component not found: " + requestLine.componentId()));
                OfferCompensationLine line = new OfferCompensationLine();
                line.setCompensation(comp);
                line.setComponent(sc);
                line.setAmount(requestLine.amount());
                line.setFrequency(requestLine.frequency());
                comp.getOfferCompensationLine().add(line);
            }
            offerCompensationRepository.save(comp);
        }
        return OfferDto.from(saved);
    }

    private BigDecimal calculateAnnualCtc(List<OfferCompensationLineRequest> lines) {
        return lines.stream()
                .map(this::toAnnualAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal toAnnualAmount(OfferCompensationLineRequest line) {
        BigDecimal amount = line.amount() != null ? line.amount() : BigDecimal.ZERO;

        return switch (line.frequency()) {
            case MONTHLY -> amount.multiply(BigDecimal.valueOf(12));
            case YEARLY, ONE_TIME -> amount;
        };
    }

    @Transactional
    public OfferDto action(Long id, @Valid OfferActionRequest req) {
        requireHrAdmin();
        if (req == null || req.action() == null) {
            throw new IllegalArgumentException("Action is required");
        }
        return switch (req.action()) {
            case SEND -> releaseOffer(id, false);
            case RESEND -> releaseOffer(id, true);
            case ACCEPT -> acceptOffer(id);
            case REJECT -> rejectOffer(id);
            case JOIN -> markJoined(id, req.join());
        };
    }

    @Transactional
    public OfferDto releaseOffer(Long id, boolean forceResend) {
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

        o.setStatus(OfferStatus.SENT);
        recordEvent(o, forceResend ? OfferEventAction.RESENT : OfferEventAction.RELEASED, u.getId(), null);
        if (o.getOfferReleaseDate() == null) {
            o.setOfferReleaseDate(LocalDate.now());
        }

        OfferPdfDownload d = generatePdfDownload(id);
        try {
            offerEmailService.sendOffer(o.getCandidateEmail(), o.getCandidateName(), d.bytes(), d.filename());
            recordEvent(o, OfferEventAction.EMAIL_SENT, u.getId(), null);
        } catch (Exception ex) {
            recordEvent(o, OfferEventAction.EMAIL_FAILED, u.getId(), truncate(ex.getMessage(), 2000));
        }

        return OfferDto.from(jobOfferRepository.save(o));
    }

    @Transactional
    public OfferPdfDownload generatePdfDownload(Long id) {
        requireHrAdmin();
        JobOffer jobOffer = jobOfferRepository.findWithDepartmentAndDesignationById(id)
                .orElseThrow(() -> new IllegalArgumentException("Offer not found: " + id));
        OfferCompensation comp = offerCompensationRepository.findByOfferId(jobOffer.getId()).orElse(null);
        List<OfferPdfService.OfferCompLine> lines = List.of();
        if (comp != null && comp.getOfferCompensationLine() != null && !comp.getOfferCompensationLine().isEmpty()) {
            String currency = comp.getCurrency() != null ? comp.getCurrency() : (jobOffer.getCurrency() != null ? jobOffer.getCurrency() : "");
            lines = comp.getOfferCompensationLine().stream().map(l -> {
                String label = l.getComponent() != null
                        ? l.getComponent().getName()
                        : "Component";
                return new OfferPdfService.OfferCompLine(label, l.getAmount());
            }).toList();
        }
        OfferLetterPdfModel model = new OfferLetterPdfModel(
                jobOffer.getEmployeeType(),
                jobOffer.getCandidateName(),
                jobOffer.getCandidateEmail(),
                jobOffer.getCandidateMobile(),
                jobOffer.getJoiningDate(),
                jobOffer.getOfferReleaseDate(),
                jobOffer.getProbationPeriodMonths() != null ? jobOffer.getProbationPeriodMonths() : 0,
                jobOffer.getDesignation() != null ? jobOffer.getDesignation().getName() : "—",
                jobOffer.getDepartment() != null ? jobOffer.getDepartment().getName() : "—",
                jobOffer.getAnnualCtc(),
                lines
        );
        byte[] pdf = offerPdfService.generateOfferLetter(model);
//        transactionTemplate.executeWithoutResult(status -> {
////            jobOffer = jobOfferRepository.findById(id)
////                    .orElseThrow(() -> new IllegalArgumentException("Offer not found: " + id));
////            jobOffer.setPdfGeneratedAt(Instant.now()); TODO:: check why we need this code.
//        });
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
    public OfferDto acceptOffer(Long id) {
        requireHrAdmin();
        User u = currentUserService.requireCurrentUser();
        JobOffer o = jobOfferRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Offer not found: " + id));
        if (o.getStatus() == OfferStatus.JOINED) {
            throw new IllegalArgumentException("Offer already joined");
        }
        o.setStatus(OfferStatus.ACCEPTED);
        recordEvent(o, OfferEventAction.ACCEPTED, u.getId(), null);
        return OfferDto.from(jobOfferRepository.save(o));
    }

    @Transactional
    public OfferDto rejectOffer(Long id) {
        requireHrAdmin();
        User u = currentUserService.requireCurrentUser();
        JobOffer o = jobOfferRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Offer not found: " + id));
        if (o.getStatus() == OfferStatus.JOINED) {
            throw new IllegalArgumentException("Offer already joined");
        }
        o.setStatus(OfferStatus.REJECTED);
        recordEvent(o, OfferEventAction.REJECTED, u.getId(), null);
        return OfferDto.from(jobOfferRepository.save(o));
    }

    @Transactional
    public OfferDto markJoined(Long id, @Valid MarkJoinedRequest body) {
        requireHrAdmin();
        User u = currentUserService.requireCurrentUser();
        JobOffer offer = jobOfferRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Offer not found: " + id));
        if (body == null || body.actualJoiningDate() == null) {
            throw new IllegalArgumentException("Actual joining date is required");
        }
        if (offer.getStatus() != OfferStatus.ACCEPTED) {
            throw new IllegalArgumentException("Only accepted offers can be marked joined");
        }
        if (offer.getEmployee() != null) {
            throw new IllegalArgumentException("Employee already created for this offer");
        }
        if (offer.getCandidateEmail() == null || offer.getCandidateEmail().isBlank()) {
            throw new IllegalArgumentException("Candidate email is required to create employee");
        }
        if (offer.getCandidateMobile() == null || offer.getCandidateMobile().isBlank()) {
            throw new IllegalArgumentException("Candidate mobile number is required to create employee");
        }

        NameParts np = splitName(offer.getCandidateName());
        EmployeeRequest empReq = new EmployeeRequest(
                null,
                np.firstName(),
                np.lastName(),
                offer.getCandidateEmail(),
                offer.getCandidateMobile(),
                offer.getDepartment() != null ? offer.getDepartment().getId() : null,
                offer.getDesignation() != null ? offer.getDesignation().getId() : null,
                body.actualJoiningDate()
        );
        var createdEmployee = employeeService.create(empReq);

        offer.setEmployee(employeeRepository.getReferenceById(createdEmployee.id()));
        offer.setStatus(OfferStatus.JOINED);
        offer.setActualJoiningDate(body.actualJoiningDate());
        recordEvent(offer, OfferEventAction.JOINED, u.getId(), null);

        OfferCompensation offerComp = offerCompensationRepository.findByOfferId(offer.getId())
                .orElseThrow(() -> new IllegalArgumentException("Offer compensation is required to create employee compensation"));
        if (offerComp.getOfferCompensationLine() == null || offerComp.getOfferCompensationLine().isEmpty()) {
            throw new IllegalArgumentException("Offer compensation lines are required");
        }

        EmployeeCompensation c = new EmployeeCompensation();
        c.setEmployee(employeeRepository.getReferenceById(createdEmployee.id()));
        LocalDate effectiveDate = body.compensationEffectiveFrom() != null ? body.compensationEffectiveFrom() : body.actualJoiningDate();
        c.setEffectiveFrom(effectiveDate);
        c.setCurrency(offerComp.getCurrency() != null ? offerComp.getCurrency() : (offer.getCurrency() != null ? offer.getCurrency() : "INR"));
        c.setNotes("Created from offer #" + offer.getId());

        for (OfferCompensationLine ol : offerComp.getOfferCompensationLine()) {
            EmployeeCompensationLine nl = new EmployeeCompensationLine();
            nl.setCompensation(c);
            nl.setComponent(ol.getComponent());
            nl.setAmount(ol.getAmount() != null ? ol.getAmount() : BigDecimal.ZERO);
            nl.setFrequency(ol.getFrequency() != null ? ol.getFrequency() : com.hrms.compensation.CompensationFrequency.MONTHLY);
            c.getLines().add(nl);
        }

        c.setAnnualCtc(c.calculateAnnualCtc());
        EmployeeCompensation saved = employeeCompensationRepository.save(c);
        compensationService.syncToSalaryStructure(saved.getId());

        return OfferDto.from(jobOfferRepository.save(offer));
    }

    private void recordEvent(JobOffer offer, OfferEventAction action, Long byUserId, String remark) {
        JobOfferEvent e = new JobOfferEvent();
        e.setOffer(offer);
        e.setAction(action);
        e.setActionByUserId(byUserId);
        e.setRemark(remark);
        jobOfferEventRepository.save(e);
    }

    public record CsvExport(String filename, String csv) {}

    @Transactional(readOnly = true)
    public CsvExport exportOffersCsv(String status, String employeeType, String q, Long departmentId, Long designationId) {
        requireHrAdmin();
        Specification<JobOffer> spec = OfferSpecifications.build(status, employeeType, q, departmentId, designationId);
        List<OfferDto> rows = jobOfferRepository.findAll(spec).stream().map(OfferDto::from).toList();
        String csv = OfferCsvExporter.toCsv(rows);
        String stamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(LocalDateTime.now());
        return new CsvExport("offers_" + stamp + ".csv", csv);
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
