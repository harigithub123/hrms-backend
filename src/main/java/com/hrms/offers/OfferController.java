package com.hrms.offers;

import com.hrms.offers.dto.*;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/offers")
@PreAuthorize("hasAnyRole('HR','ADMIN')")
public class OfferController {

    private final OfferService offerService;

    public OfferController(OfferService offerService) {
        this.offerService = offerService;
    }

    private static String toEmployeeTypeDisplayName(String employeeType) {
        if (employeeType == null) return null;
        return switch (employeeType) {
            case "PERMANENT_FULL_TIME" -> "Permanent - Full time";
            case "PERMANENT_PART_TIME" -> "Permanent - Part time";
            case "CONTRACT" -> "Contract";
            default -> employeeType;
        };
    }

    @GetMapping
    public List<OfferDto> listOffers() {
        return offerService.listOffers().stream()
                .map(o -> new OfferDto(
                        o.id(),
                        o.candidateName(),
                        o.candidateEmail(),
                        o.candidateMobile(),
                        o.status(),
                        toEmployeeTypeDisplayName(o.employeeType()),
                        o.departmentId(),
                        o.departmentName(),
                        o.designationId(),
                        o.designationName(),
                        o.joiningDate(),
                        o.offerReleaseDate(),
                        o.actualJoiningDate(),
                        o.probationPeriodMonths(),
                        o.employeeId(),
                        o.createdAt()
                ))
                .toList();
    }

    @GetMapping("/paged")
    public Page<OfferDto> listOffersPaged(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String employeeType,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long designationId,
            @PageableDefault(size = 10, sort = "id") Pageable pageable
    ) {
        return offerService.listOffersPaged(status, employeeType, q, departmentId, designationId, pageable)
                .map(o -> new OfferDto(
                        o.id(),
                        o.candidateName(),
                        o.candidateEmail(),
                        o.candidateMobile(),
                        o.status(),
                        toEmployeeTypeDisplayName(o.employeeType()),
                        o.departmentId(),
                        o.departmentName(),
                        o.designationId(),
                        o.designationName(),
                        o.joiningDate(),
                        o.offerReleaseDate(),
                        o.actualJoiningDate(),
                        o.probationPeriodMonths(),
                        o.employeeId(),
                        o.createdAt()
                ));
    }

    /**
     * Declared before {@code /{id}} so paths like {@code /api/offers/5/pdf} are not captured as id="5/pdf".
     */
    @GetMapping("/{id:\\d+}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable Long id) {
        OfferService.OfferPdfDownload d = offerService.generatePdfDownload(id);
        String fn = d.filename();
        String disposition = "attachment; filename=\"" + fn.replace("\"", "") + "\"";
        byte[] bytes = d.bytes();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(bytes.length)
                .body(bytes);
    }

    @GetMapping("/{id:\\d+}")
    public OfferDto get(@PathVariable Long id) {
        return offerService.getOffer(id);
    }

    @PostMapping
    public OfferDto create(@Valid @RequestBody OfferCreateRequest req) {
        return offerService.createOffer(req);
    }

    @PostMapping("/{id:\\d+}/refresh-body")
    public OfferDto refreshBody(@PathVariable Long id) {
        return offerService.refreshBody(id);
    }

    @PostMapping("/{id:\\d+}/action")
    public OfferDto action(@PathVariable Long id, @Valid @RequestBody OfferActionRequest body) {
        return offerService.action(id, body);
    }

    @GetMapping(value = "/export.csv", produces = "text/csv")
    public ResponseEntity<String> exportCsv(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String employeeType,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long designationId
    ) {
        OfferService.CsvExport export = offerService.exportOffersCsv(status, employeeType, q, departmentId, designationId);
        String disposition = "attachment; filename=\"" + export.filename().replace("\"", "") + "\"";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .contentType(MediaType.valueOf("text/csv"))
                .body(export.csv());
    }
}
