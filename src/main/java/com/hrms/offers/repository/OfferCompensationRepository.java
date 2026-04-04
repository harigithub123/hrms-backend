package com.hrms.offers.repository;

import com.hrms.offers.entity.OfferCompensation;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OfferCompensationRepository extends JpaRepository<OfferCompensation, Long> {
    @EntityGraph(attributePaths = {"offerCompensationLine", "offerCompensationLine.component"})
    Optional<OfferCompensation> findByOfferId(Long offerId);
}

