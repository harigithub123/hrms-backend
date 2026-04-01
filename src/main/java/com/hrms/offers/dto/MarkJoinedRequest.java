package com.hrms.offers.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.AssertTrue;

import java.time.LocalDate;

/**
 * HR attestation and dates when finalizing join from an accepted offer.
 *
 * @param compensationEffectiveFrom Compensation / salary structure effective date; defaults to offer join date when null.
 * @param actualJoiningDate Actual date the employee joined (used to store on offer and to create employee record).
 * @param confirmCandidateAcceptedOffer Must be true: HR confirms candidate accepted (consent to create employment record).
 */
public record MarkJoinedRequest(
        LocalDate compensationEffectiveFrom,
        @NotNull LocalDate actualJoiningDate,
        Boolean confirmCandidateAcceptedOffer
) {
    @AssertTrue(message = "Must confirm candidate accepted offer")
    public boolean isCandidateAcceptedConfirmed() {
        return Boolean.TRUE.equals(confirmCandidateAcceptedOffer);
    }
}
