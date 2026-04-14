package com.hrms.payroll.repository;

import com.hrms.payroll.entity.SalaryComponent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SalaryComponentRepository extends JpaRepository<SalaryComponent, Long> {

    List<SalaryComponent> findByActiveTrueOrderBySortOrderAsc();

    Optional<SalaryComponent> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);
}
