package com.hrms.offers.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * One unified endpoint payload for offer lifecycle actions.
 */
public record OfferActionRequest(
        @NotNull OfferAction action,
        @Valid MarkJoinedRequest join
) {
}

