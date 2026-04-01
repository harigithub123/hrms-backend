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
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
            CompensationService compensationService,
            LeaveTypeRepository leaveTypeRepository,
            LeaveBalanceRepository leaveBalanceRepository
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
            String q,
            Long departmentId,
            Long designationId,
            Pageable pageable
    ) {
        requireHrAdmin();
        Specification<JobOffer> spec = OfferSpecifications.build(status, employeeType, q, departmentId, designationId);
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
        o.setEmployeeType(req.employeeType());
        o.setDepartment(req.departmentId() != null ? departmentRepository.getReferenceById(req.departmentId()) : null);
        o.setDesignation(req.designationId() != null ? designationRepository.getReferenceById(req.designationId()) : null);
        o.setJoiningDate(req.joiningDate());
        o.setProbationPeriodMonths(req.probationPeriodMonths());
        o.setJoiningBonus(req.joiningBonus());
        o.setYearlyBonus(req.yearlyBonus());
        o.setStatus(OfferStatus.DRAFT);
        JobOffer saved = jobOfferRepository.save(o);

        List<OfferCompensationLineRequest> lines = req.compensationLines() != null ? req.compensationLines() : List.of();
        if (!lines.isEmpty()) {
            Map<Long, BigDecimal> uniqueLines = new LinkedHashMap<>();
            for (OfferCompensationLineRequest lr : lines) {
                if (lr == null || lr.componentId() == null) continue;
                BigDecimal amt = lr.amount() != null ? lr.amount() : BigDecimal.ZERO;
                uniqueLines.merge(lr.componentId(), amt, BigDecimal::add);
            }
            if (uniqueLines.isEmpty()) {
                return OfferDto.from(saved);
            }

            OfferCompensation comp = new OfferCompensation();
            comp.setOffer(saved);
            comp.setCurrency(saved.getCurrency() != null ? saved.getCurrency() : "INR");
            for (Map.Entry<Long, BigDecimal> e : uniqueLines.entrySet()) {
                SalaryComponent sc = salaryComponentRepository.findById(e.getKey())
                        .orElseThrow(() -> new IllegalArgumentException("Salary component not found: " + e.getKey()));
                OfferCompensationLine line = new OfferCompensationLine();
                line.setCompensation(comp);
                line.setComponent(sc);
                line.setAmount(e.getValue());
                comp.getOfferCompensationLine().add(line);
            }
            offerCompensationRepository.save(comp);
        }
        return OfferDto.from(saved);
    }

    @Transactional
    public OfferDto refreshBody(Long id) {
        requireHrAdmin();
        JobOffer o = jobOfferRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Offer not found: " + id));
        return OfferDto.from(jobOfferRepository.save(o));
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

    /**
     * Filename pattern: {@code {24hex}_{CandidateName}_Offer_Letter_{yyyyMMdd}_{HHmmss}.pdf}
     * (e.g. {@code 1642233994a61e2806cb4250_HariNale_Offer_Letter_20220115_133603.pdf})
     * <p>PDF bytes are built outside a DB transaction so long-running work does not hold connections.</p>
     */
    @Transactional
    public OfferPdfDownload generatePdfDownload(Long id) {
        requireHrAdmin();
        JobOffer o = jobOfferRepository.findWithDepartmentAndDesignationById(id)
                .orElseThrow(() -> new IllegalArgumentException("Offer not found: " + id));
        jobOfferRepository.save(o);
        OfferCompensation comp = offerCompensationRepository.findByOfferId(o.getId()).orElse(null);
        List<OfferPdfService.OfferCompLine> lines = List.of();
        if (comp != null && comp.getOfferCompensationLine() != null && !comp.getOfferCompensationLine().isEmpty()) {
            String currency = comp.getCurrency() != null ? comp.getCurrency() : (o.getCurrency() != null ? o.getCurrency() : "");
            lines = comp.getOfferCompensationLine().stream().map(l -> {
                String label = l.getComponent() != null
                        ? l.getComponent().getName()
                        : "Component";
                return new OfferPdfService.OfferCompLine(label, l.getAmount());
            }).toList();
        }
        OfferPdfService.OfferLetterPdfModel model = new OfferPdfService.OfferLetterPdfModel(
                o.getEmployeeType(),
                o.getCandidateName(),
                o.getCandidateEmail(),
                o.getCandidateMobile(),
                o.getJoiningDate(),
                o.getOfferReleaseDate(),
                o.getProbationPeriodMonths() != null ? o.getProbationPeriodMonths() : 0,
                o.getJoiningBonus(),
                o.getYearlyBonus(),
                o.getDesignation() != null ? o.getDesignation().getName() : "—",
                o.getDepartment() != null ? o.getDepartment().getName() : "—",
                o.getAnnualCtc(),
                lines
        );
        byte[] pdf = offerPdfService.generateOfferLetter(model);
//        transactionTemplate.executeWithoutResult(status -> {
////            o = jobOfferRepository.findById(id)
////                    .orElseThrow(() -> new IllegalArgumentException("Offer not found: " + id));
////            o.setPdfGeneratedAt(Instant.now()); TODO:: check why we need this code.
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
        JobOffer o = jobOfferRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Offer not found: " + id));
        if (body == null || body.actualJoiningDate() == null) {
            throw new IllegalArgumentException("Actual joining date is required");
        }
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
                body.actualJoiningDate()
        );
        var createdEmployee = employeeService.create(empReq);

        o.setEmployee(employeeRepository.getReferenceById(createdEmployee.id()));
        o.setStatus(OfferStatus.JOINED);
        o.setActualJoiningDate(body.actualJoiningDate());
        recordEvent(o, OfferEventAction.JOINED, u.getId(), null);

        OfferCompensation offerComp = offerCompensationRepository.findByOfferId(o.getId())
                .orElseThrow(() -> new IllegalArgumentException("Offer compensation is required to create employee compensation"));
        if (offerComp.getOfferCompensationLine() == null || offerComp.getOfferCompensationLine().isEmpty()) {
            throw new IllegalArgumentException("Offer compensation lines are required");
        }

        EmployeeCompensation c = new EmployeeCompensation();
        c.setEmployee(employeeRepository.getReferenceById(createdEmployee.id()));
        LocalDate eff = body.compensationEffectiveFrom() != null ? body.compensationEffectiveFrom() : body.actualJoiningDate();
        c.setEffectiveFrom(eff);
        c.setCurrency(offerComp.getCurrency() != null ? offerComp.getCurrency() : (o.getCurrency() != null ? o.getCurrency() : "INR"));
        c.setAnnualCtc(offerComp.getAnnualCtc() != null ? offerComp.getAnnualCtc() : o.getAnnualCtc());
        c.setNotes("Created from offer #" + o.getId());

        for (OfferCompensationLine ol : offerComp.getOfferCompensationLine()) {
            EmployeeCompensationLine nl = new EmployeeCompensationLine();
            nl.setCompensation(c);
            nl.setComponent(ol.getComponent());
            nl.setAmount(ol.getAmount() != null ? ol.getAmount() : BigDecimal.ZERO);
            nl.setFrequency(com.hrms.compensation.CompensationFrequency.MONTHLY);
            c.getLines().add(nl);
        }

        // Bonuses as separate lines with frequency.
        if (o.getJoiningBonus() != null && BigDecimal.ZERO.compareTo(o.getJoiningBonus()) != 0) {
            SalaryComponent sc = salaryComponentRepository.findByCodeIgnoreCase("JOINING_BONUS")
                    .orElseThrow(() -> new IllegalStateException("Salary component missing: JOINING_BONUS"));
            EmployeeCompensationLine bl = new EmployeeCompensationLine();
            bl.setCompensation(c);
            bl.setComponent(sc);
            bl.setAmount(o.getJoiningBonus());
            bl.setFrequency(com.hrms.compensation.CompensationFrequency.ONE_TIME);
            bl.setPayableOn(body.actualJoiningDate());
            c.getLines().add(bl);
        }
        if (o.getYearlyBonus() != null && BigDecimal.ZERO.compareTo(o.getYearlyBonus()) != 0) {
            SalaryComponent sc = salaryComponentRepository.findByCodeIgnoreCase("ANNUAL_BONUS")
                    .orElseThrow(() -> new IllegalStateException("Salary component missing: ANNUAL_BONUS"));
            EmployeeCompensationLine bl = new EmployeeCompensationLine();
            bl.setCompensation(c);
            bl.setComponent(sc);
            bl.setAmount(o.getYearlyBonus());
            bl.setFrequency(com.hrms.compensation.CompensationFrequency.YEARLY);
            bl.setPayableOn(null);
            c.getLines().add(bl);
        }

        EmployeeCompensation saved = employeeCompensationRepository.save(c);
        compensationService.syncToSalaryStructure(saved.getId());

        return OfferDto.from(jobOfferRepository.save(o));
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
