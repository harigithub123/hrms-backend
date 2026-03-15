package com.hrms.org.repository;

import com.hrms.org.entity.Designation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DesignationRepository extends JpaRepository<Designation, Long> {

    Optional<Designation> findByCode(String code);

    boolean existsByCode(String code);
}
