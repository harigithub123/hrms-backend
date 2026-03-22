package com.hrms.offers.repository;

import com.hrms.offers.entity.JobOffer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobOfferRepository extends JpaRepository<JobOffer, Long> {
    List<JobOffer> findAllByOrderByIdDesc();
}
