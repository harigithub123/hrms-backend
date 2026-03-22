package com.hrms.offers.dto;

import com.hrms.offers.entity.OfferTemplate;

import java.time.Instant;

public record OfferTemplateDto(Long id, String name, String bodyHtml, boolean active, Instant createdAt) {
    public static OfferTemplateDto from(OfferTemplate t) {
        return new OfferTemplateDto(t.getId(), t.getName(), t.getBodyHtml(), t.isActive(), t.getCreatedAt());
    }
}
