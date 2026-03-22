package com.hrms.offers;

import com.hrms.auth.entity.User;
import com.hrms.offers.dto.*;
import com.hrms.offers.entity.JobOffer;
import com.hrms.offers.entity.OfferTemplate;
import com.hrms.offers.repository.JobOfferRepository;
import com.hrms.offers.repository.OfferTemplateRepository;
import com.hrms.org.repository.DepartmentRepository;
import com.hrms.org.repository.DesignationRepository;
import com.hrms.org.repository.EmployeeRepository;
import com.hrms.security.CurrentUserService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.Instant;
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
    private final TransactionTemplate transactionTemplate;

    public OfferService(
            OfferTemplateRepository templateRepository,
            JobOfferRepository jobOfferRepository,
            DepartmentRepository departmentRepository,
            DesignationRepository designationRepository,
            EmployeeRepository employeeRepository,
            CurrentUserService currentUserService,
            OfferPdfService offerPdfService,
            TransactionTemplate transactionTemplate
    ) {
        this.templateRepository = templateRepository;
        this.jobOfferRepository = jobOfferRepository;
        this.departmentRepository = departmentRepository;
        this.designationRepository = designationRepository;
        this.employeeRepository = employeeRepository;
        this.currentUserService = currentUserService;
        this.offerPdfService = offerPdfService;
        this.transactionTemplate = transactionTemplate;
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
        o.setDepartment(req.departmentId() != null ? departmentRepository.getReferenceById(req.departmentId()) : null);
        o.setDesignation(req.designationId() != null ? designationRepository.getReferenceById(req.designationId()) : null);
        o.setManager(req.managerId() != null ? employeeRepository.getReferenceById(req.managerId()) : null);
        o.setJoinDate(req.joinDate());
        o.setAnnualCtc(req.annualCtc());
        o.setCurrency(req.currency() != null && !req.currency().isBlank() ? req.currency().trim() : "INR");
        o.setStatus(OfferStatus.DRAFT);
        o.setBodyHtml(mergeBody(o));
        return JobOfferDto.from(jobOfferRepository.save(o));
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
    public JobOfferDto sendOffer(Long id) {
        requireHrAdmin();
        JobOffer o = jobOfferRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Offer not found: " + id));
        o.setBodyHtml(mergeBody(o));
        o.setStatus(OfferStatus.SENT);
        return JobOfferDto.from(jobOfferRepository.save(o));
    }

    /**
     * Filename pattern: {@code {24hex}_{CandidateName}_Offer_Letter_{yyyyMMdd}_{HHmmss}.pdf}
     * (e.g. {@code 1642233994a61e2806cb4250_HariNale_Offer_Letter_20220115_133603.pdf})
     * <p>PDF bytes are built outside a DB transaction so long-running work does not hold connections.</p>
     */
    public OfferPdfDownload generatePdfDownload(Long id) {
        requireHrAdmin();
        OfferPdfMaterial material = transactionTemplate.execute(status -> {
            JobOffer o = jobOfferRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Offer not found: " + id));
            if (o.getBodyHtml() == null || o.getBodyHtml().isBlank()) {
                o.setBodyHtml(mergeBody(o));
                jobOfferRepository.save(o);
            }
            return new OfferPdfMaterial(o.getCandidateName(), o.getBodyHtml());
        });
        byte[] pdf;
        try {
            pdf = offerPdfService.buildFromHtml(material.bodyHtml());
        } catch (IOException e) {
            throw new IllegalStateException("PDF generation failed: " + e.getMessage(), e);
        }
        transactionTemplate.executeWithoutResult(status -> {
            JobOffer o = jobOfferRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Offer not found: " + id));
            o.setPdfGeneratedAt(Instant.now());
        });
        return new OfferPdfDownload(pdf, buildOfferPdfFilename(material.candidateName()));
    }

    private record OfferPdfMaterial(String candidateName, String bodyHtml) {}

    public static String buildOfferPdfFilename(JobOffer offer) {
        return buildOfferPdfFilename(offer.getCandidateName());
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
        JobOffer o = jobOfferRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Offer not found: " + id));
        o.setStatus(OfferStatus.ACCEPTED);
        return JobOfferDto.from(jobOfferRepository.save(o));
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
        return raw
                .replace("{{candidateName}}", o.getCandidateName() != null ? o.getCandidateName() : "")
                .replace("{{department}}", dept)
                .replace("{{designation}}", des)
                .replace("{{managerName}}", mgr)
                .replace("{{joinDate}}", join)
                .replace("{{annualCtc}}", ctc)
                .replace("{{currency}}", o.getCurrency() != null ? o.getCurrency() : "");
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
