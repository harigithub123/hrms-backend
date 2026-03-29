package com.hrms.leave.repository;

import com.hrms.leave.LeaveRequestStatus;
import com.hrms.leave.entity.LeaveRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long>, JpaSpecificationExecutor<LeaveRequest> {

    List<LeaveRequest> findByEmployeeIdOrderByRequestedAtDesc(Long employeeId);

    List<LeaveRequest> findByEmployeeIdInOrderByRequestedAtDesc(List<Long> employeeIds);

    @Query("""
            SELECT r FROM LeaveRequest r
            WHERE r.startDate <= :to AND r.endDate >= :from
            AND r.status IN :statuses
            AND (:employeeId IS NULL OR r.employee.id = :employeeId)
            ORDER BY r.startDate
            """)
    List<LeaveRequest> findOverlappingForCalendar(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("statuses") List<LeaveRequestStatus> statuses,
            @Param("employeeId") Long employeeId
    );

    List<LeaveRequest> findByStatusAndEmployeeIdIn(LeaveRequestStatus status, List<Long> employeeIds);

    @Query("""
            SELECT r FROM LeaveRequest r
            WHERE r.employee.id = :employeeId
            AND r.status = :status
            AND r.startDate >= :fromInclusive
            AND r.startDate <= :toInclusive
            AND r.decidedAt IS NOT NULL
            ORDER BY r.decidedAt ASC, r.id ASC
            """)
    List<LeaveRequest> findApprovedForLedger(
            @Param("employeeId") Long employeeId,
            @Param("status") LeaveRequestStatus status,
            @Param("fromInclusive") LocalDate fromInclusive,
            @Param("toInclusive") LocalDate toInclusive
    );
}
