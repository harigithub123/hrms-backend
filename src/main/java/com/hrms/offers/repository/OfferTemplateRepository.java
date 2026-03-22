package com.hrms.offers.repository;

import com.hrms.offers.entity.OfferTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OfferTemplateRepository extends JpaRepository<OfferTemplate, Long> {
    List<OfferTemplate> findByActiveTrueOrderByNameAsc();
}
