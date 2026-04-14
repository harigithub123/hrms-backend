package com.hrms.payroll.repository;

import com.hrms.payroll.entity.SalaryStructure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface SalaryStructureRepository extends JpaRepository<SalaryStructure, Long> {

    @Query("""
            SELECT DISTINCT s FROM SalaryStructure s
            LEFT JOIN FETCH s.lines l
            LEFT JOIN FETCH l.component
            WHERE s.employee.id = :employeeId AND s.effectiveFrom <= :asOf
            ORDER BY s.effectiveFrom DESC
            """)
    List<SalaryStructure> findCandidatesForPayroll(
            @Param("employeeId") Long employeeId,
            @Param("asOf") LocalDate asOf
    );

    List<SalaryStructure> findByEmployeeIdOrderByEffectiveFromDesc(Long employeeId);
}
