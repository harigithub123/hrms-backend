package com.hrms.leave.repository;

import com.hrms.leave.entity.LeaveBalanceAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LeaveBalanceAdjustmentRepository extends JpaRepository<LeaveBalanceAdjustment, Long> {

    @Query("""
            SELECT a FROM LeaveBalanceAdjustment a
            JOIN FETCH a.leaveBalance lb
            JOIN FETCH lb.leaveType
            WHERE lb.employee.id = :employeeId AND lb.year = :year
            ORDER BY a.createdAt DESC
            """)
    List<LeaveBalanceAdjustment> findHistoryForEmployeeYear(@Param("employeeId") Long employeeId, @Param("year") int year);
}
