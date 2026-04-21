package com.hrms.payroll.repository;

import com.hrms.payroll.entity.PayRun;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PayRunRepository extends JpaRepository<PayRun, Long> {

    boolean existsByPayYearAndPayMonth(int payYear, int payMonth);
}
