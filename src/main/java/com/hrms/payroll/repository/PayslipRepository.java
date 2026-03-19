package com.hrms.payroll.repository;

import com.hrms.payroll.entity.Payslip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PayslipRepository extends JpaRepository<Payslip, Long> {

    List<Payslip> findByPayRunIdOrderByEmployeeId(Long payRunId);

    List<Payslip> findByEmployeeIdOrderByIdDesc(Long employeeId);

    @Query("SELECT DISTINCT p FROM Payslip p LEFT JOIN FETCH p.lines WHERE p.id = :id")
    Optional<Payslip> findByIdWithLines(@Param("id") Long id);
}
