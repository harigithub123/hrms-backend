package com.hrms.org.repository;

import com.hrms.org.entity.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    long countByManagerId(Long managerId);

    List<Employee> findByManagerId(Long managerId);

    List<Employee> findByDepartmentId(Long departmentId);

    @Query("""
            SELECT e FROM Employee e
            WHERE LOWER(e.firstName) LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(e.lastName) LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(COALESCE(e.employeeCode, '')) LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(COALESCE(e.email, '')) LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(COALESCE(e.mobileNumber, '')) LIKE LOWER(CONCAT('%', :q, '%'))
            """)
    Page<Employee> searchByText(@Param("q") String q, Pageable pageable);
}
