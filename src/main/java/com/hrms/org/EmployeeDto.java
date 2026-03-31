package com.hrms.org;

import java.time.LocalDate;

public record EmployeeDto(
        Long id,
        String employeeCode,
        String firstName,
        String lastName,
        String email,
        String mobileNumber,
        Long departmentId,
        String departmentName,
        Long designationId,
        String designationName,
        Long managerId,
        String managerName,
        LocalDate joinedAt
) {
    public static EmployeeDto from(com.hrms.org.entity.Employee e) {
        return new EmployeeDto(
                e.getId(),
                e.getEmployeeCode(),
                e.getFirstName(),
                e.getLastName(),
                e.getEmail(),
                e.getMobileNumber(),
                e.getDepartment() != null ? e.getDepartment().getId() : null,
                e.getDepartment() != null ? e.getDepartment().getName() : null,
                e.getDesignation() != null ? e.getDesignation().getId() : null,
                e.getDesignation() != null ? e.getDesignation().getName() : null,
                e.getManager() != null ? e.getManager().getId() : null,
                e.getManager() != null ? (e.getManager().getFirstName() + " " + e.getManager().getLastName()).trim() : null,
                e.getJoinedAt()
        );
    }
}
