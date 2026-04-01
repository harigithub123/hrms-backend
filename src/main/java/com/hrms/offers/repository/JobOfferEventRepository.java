package com.hrms.offers.repository;

import com.hrms.offers.entity.JobOfferEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobOfferEventRepository extends JpaRepository<JobOfferEvent, Long> {
}

