package com.hrms.offers.repository;

import com.hrms.offers.entity.JobOffer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import java.util.Optional;

public interface JobOfferRepository extends JpaRepository<JobOffer, Long>, JpaSpecificationExecutor<JobOffer> {
    List<JobOffer> findAllByOrderByIdDesc();

    @EntityGraph(attributePaths = {"department", "designation"})
    Optional<JobOffer> findWithDepartmentAndDesignationById(Long id);
}
