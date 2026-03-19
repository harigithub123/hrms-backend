package com.hrms.payroll.dto;

import com.hrms.payroll.SalaryComponentKind;
import com.hrms.payroll.entity.SalaryComponent;

public record SalaryComponentDto(
        Long id,
        String code,
        String name,
        SalaryComponentKind kind,
        int sortOrder,
        boolean active
) {
    public static SalaryComponentDto from(SalaryComponent c) {
        return new SalaryComponentDto(
                c.getId(),
                c.getCode(),
                c.getName(),
                c.getKind(),
                c.getSortOrder(),
                c.isActive()
        );
    }
}
