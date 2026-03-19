package com.hrms.payroll.repository;

import com.hrms.payroll.entity.EmployeeSalaryStructure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface EmployeeSalaryStructureRepository extends JpaRepository<EmployeeSalaryStructure, Long> {

    @Query("""
            SELECT DISTINCT s FROM EmployeeSalaryStructure s
            LEFT JOIN FETCH s.lines l
            LEFT JOIN FETCH l.component
            WHERE s.employee.id = :employeeId AND s.effectiveFrom <= :asOf
            ORDER BY s.effectiveFrom DESC
            """)
    List<EmployeeSalaryStructure> findCandidatesForPayroll(
            @Param("employeeId") Long employeeId,
            @Param("asOf") LocalDate asOf
    );

    List<EmployeeSalaryStructure> findByEmployeeIdOrderByEffectiveFromDesc(Long employeeId);
}
