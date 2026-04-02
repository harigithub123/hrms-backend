package com.hrms.offers;

import com.hrms.offers.entity.JobOffer;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class OfferSpecifications {

    private OfferSpecifications() {}

    public static Specification<JobOffer> build(
            String status,
            String employeeType,
            String searchQuery,
            Long departmentId,
            Long designationId
    ) {
        return (root, query, cb) -> {
            List<Predicate> preds = new ArrayList<>();

            if (status != null && !status.isBlank()) {
                preds.add(cb.equal(root.get("status"), com.hrms.offers.OfferStatus.valueOf(status.trim())));
            }
            if (employeeType != null && !employeeType.isBlank()) {
                preds.add(cb.equal(root.get("employeeType"), employeeType.trim()));
            }
            if (departmentId != null) {
                preds.add(cb.equal(root.get("department").get("id"), departmentId));
            }
            if (designationId != null) {
                preds.add(cb.equal(root.get("designation").get("id"), designationId));
            }
            if (searchQuery != null && !searchQuery.isBlank()) {
                String like = "%" + searchQuery.trim().toLowerCase() + "%";
                preds.add(cb.or(
                        cb.like(cb.lower(root.get("candidateName")), like),
                        cb.like(cb.lower(root.get("candidateEmail")), like)
                ));
            }

            return cb.and(preds.toArray(Predicate[]::new));
        };
    }
}

