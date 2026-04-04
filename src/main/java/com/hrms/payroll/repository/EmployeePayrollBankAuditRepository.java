package com.hrms.payroll.repository;

import com.hrms.payroll.entity.EmployeePayrollBankAudit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmployeePayrollBankAuditRepository extends JpaRepository<EmployeePayrollBankAudit, Long> {

    List<EmployeePayrollBankAudit> findByEmployee_IdOrderByCreatedAtDesc(Long employeeId);
}
