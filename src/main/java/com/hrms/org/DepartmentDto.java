package com.hrms.org;

public record DepartmentDto(Long id, String name, String code, String description) {

    public static DepartmentDto from(com.hrms.org.entity.Department e) {
        return new DepartmentDto(e.getId(), e.getName(), e.getCode(), e.getDescription());
    }
}
