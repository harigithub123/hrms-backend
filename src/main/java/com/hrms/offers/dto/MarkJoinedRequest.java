package com.hrms.offers.dto;

import java.time.LocalDate;

/**
 * HR attestation and dates when finalizing join from an accepted offer.
 *
 * @param compensationEffectiveFrom Compensation / salary structure effective date; defaults to offer join date when null.
 * @param confirmCandidateAcceptedOffer Must be true: HR confirms candidate accepted (consent to create employment record).
 */
public record MarkJoinedRequest(
        LocalDate compensationEffectiveFrom,
        Boolean confirmCandidateAcceptedOffer
) {}
