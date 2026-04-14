package com.hrms.payroll.repository;

import com.hrms.payroll.entity.PayrollFixedComponentAmount;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PayrollFixedComponentAmountRepository extends JpaRepository<PayrollFixedComponentAmount, Long> {

    @EntityGraph(attributePaths = "salaryComponent")
    @Override
    List<PayrollFixedComponentAmount> findAll();

    Optional<PayrollFixedComponentAmount> findBySalaryComponent_Id(Long salaryComponentId);

    void deleteBySalaryComponent_Id(Long salaryComponentId);
}
