package com.hrms.offers;

import com.hrms.offers.dto.*;
import jakarta.validation.Valid;
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

    @GetMapping("/templates")
    public List<OfferTemplateDto> listTemplates() {
        return offerService.listTemplates();
    }

    @GetMapping("/templates/all")
    public List<OfferTemplateDto> listAllTemplates() {
        return offerService.listAllTemplatesAdmin();
    }

    @PostMapping("/templates")
    public OfferTemplateDto createTemplate(@Valid @RequestBody OfferTemplateRequest req) {
        return offerService.createTemplate(req);
    }

    @PutMapping("/templates/{id}")
    public OfferTemplateDto updateTemplate(@PathVariable Long id, @Valid @RequestBody OfferTemplateRequest req) {
        return offerService.updateTemplate(id, req);
    }

    @GetMapping
    public List<JobOfferDto> listOffers() {
        return offerService.listOffers();
    }

    /**
     * Declared before {@code /{id}} so paths like {@code /api/offers/5/pdf} are not captured as id="5/pdf".
     */
    @GetMapping("/{id}/pdf")
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

    @GetMapping("/{id}")
    public JobOfferDto get(@PathVariable Long id) {
        return offerService.getOffer(id);
    }

    @PostMapping
    public JobOfferDto create(@Valid @RequestBody JobOfferCreateRequest req) {
        return offerService.createOffer(req);
    }

    @PostMapping("/{id}/refresh-body")
    public JobOfferDto refreshBody(@PathVariable Long id) {
        return offerService.refreshBody(id);
    }

    @PostMapping("/{id}/send")
    public JobOfferDto send(@PathVariable Long id) {
        return offerService.sendOffer(id);
    }

    @PostMapping("/{id}/accept")
    public JobOfferDto accept(@PathVariable Long id) {
        return offerService.acceptOffer(id);
    }
}
