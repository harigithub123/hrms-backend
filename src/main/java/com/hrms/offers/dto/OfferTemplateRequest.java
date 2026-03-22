package com.hrms.offers.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OfferTemplateRequest(
        @NotBlank @Size(max = 200) String name,
        @NotBlank String bodyHtml,
        boolean active
) {}
