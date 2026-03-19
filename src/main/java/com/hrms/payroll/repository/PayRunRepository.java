package com.hrms.payroll.repository;

import com.hrms.payroll.entity.PayRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface PayRunRepository extends JpaRepository<PayRun, Long> {

    boolean existsByPeriodStartAndPeriodEnd(LocalDate periodStart, LocalDate periodEnd);
}
