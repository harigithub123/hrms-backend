package com.hrms.advance.repository;

import com.hrms.advance.AdvanceStatus;
import com.hrms.advance.entity.SalaryAdvance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.util.List;

public interface SalaryAdvanceRepository extends JpaRepository<SalaryAdvance, Long> {

    List<SalaryAdvance> findByEmployeeIdOrderByIdDesc(Long employeeId);

    List<SalaryAdvance> findByEmployeeIdAndStatusAndOutstandingBalanceGreaterThan(
            Long employeeId, AdvanceStatus status, BigDecimal minOutstanding);

    List<SalaryAdvance> findByStatusOrderByRequestedAtAsc(AdvanceStatus status);

    List<SalaryAdvance> findAllByOrderByIdDesc();
}
