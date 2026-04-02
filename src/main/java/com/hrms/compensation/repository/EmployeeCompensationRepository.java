package com.hrms.compensation.repository;

import com.hrms.compensation.entity.EmployeeCompensation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface EmployeeCompensationRepository extends JpaRepository<EmployeeCompensation, Long> {

    @Query("""
            SELECT c FROM EmployeeCompensation c JOIN FETCH c.employee
            WHERE c.employee.id = :employeeId
            ORDER BY c.effectiveFrom DESC
            """)
    List<EmployeeCompensation> findByEmployeeIdOrderByEffectiveFromDesc(@Param("employeeId") Long employeeId);

    @Query("""
            SELECT c FROM EmployeeCompensation c JOIN FETCH c.employee
            WHERE c.employee.id = :employeeId
            AND c.effectiveFrom <= :asOf
            AND (c.effectiveTo IS NULL OR c.effectiveTo >= :asOf)
            ORDER BY c.effectiveFrom DESC
            """)
    List<EmployeeCompensation> findActiveAsOf(@Param("employeeId") Long employeeId, @Param("asOf") LocalDate asOf);

    @Query("""
            SELECT c FROM EmployeeCompensation c JOIN FETCH c.employee
            WHERE c.effectiveFrom <= :asOf
            AND (c.effectiveTo IS NULL OR c.effectiveTo >= :asOf)
            ORDER BY c.effectiveFrom DESC
            """)
    List<EmployeeCompensation> findAllActiveAsOf(@Param("asOf") LocalDate asOf);

    @Query("""
            SELECT c FROM EmployeeCompensation c JOIN FETCH c.employee
            ORDER BY c.effectiveFrom DESC
            """)
    List<EmployeeCompensation> findAllOrderByEffectiveFromDesc();
}
