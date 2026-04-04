package com.hrms.payroll.repository;

import com.hrms.payroll.entity.EmployeePayrollBank;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmployeePayrollBankRepository extends JpaRepository<EmployeePayrollBank, Long> {

    Optional<EmployeePayrollBank> findByEmployee_Id(Long employeeId);
}
