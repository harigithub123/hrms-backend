package com.hrms.org;

public record DesignationDto(Long id, String name, String code) {

    public static DesignationDto from(com.hrms.org.entity.Designation e) {
        return new DesignationDto(e.getId(), e.getName(), e.getCode());
    }
}
